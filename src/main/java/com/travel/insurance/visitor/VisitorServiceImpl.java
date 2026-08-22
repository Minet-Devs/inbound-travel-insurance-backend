package com.travel.insurance.visitor;

import com.travel.insurance.common.crypto.BlindIndexService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerRepository;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.visitor.dto.VisitorEntryExitUpdate;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import com.travel.insurance.visitor.dto.VisitorStatusUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitorServiceImpl implements VisitorService {

    private static final int MIN_COVER_DAYS = 1;
    private static final int MAX_COVER_DAYS = 365;

    private final VisitorRepository visitorRepository;
    private final VisitorMapper visitorMapper;
    private final PolicyService policyService;
    private final InsurerRepository insurerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BlindIndexService blindIndexService;

    @Override
    public VisitorResponse create(VisitorRequest request) {
        Policy policy = policyService.getEntityById(request.policyId());
        validateCoverPeriod(request);
        validatePolicyQuota(policy);
        String passportNumberHash = blindIndexService.hmac(request.passportNumber());
        if (visitorRepository.existsByPassportNumberHash(passportNumberHash)) {
            throw new IllegalStateException(
                    "Visitor already exists with passport number: " + request.passportNumber());
        }
        Visitor visitor = visitorMapper.toEntity(request);
        visitor.setPassportNumberHash(passportNumberHash);
        visitor = visitorRepository.save(visitor);
        eventPublisher.publishEvent(new VisitorCreatedEvent(visitor.getId(), visitor.getPolicyId()));
        return visitorMapper.toResponse(visitor);
    }

    @Override
    @Transactional(readOnly = true)
    public VisitorResponse getById(UUID id) {
        return visitorMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorResponse> listByPolicyId(UUID policyId) {
        return visitorRepository.findAllByPolicyId(policyId).stream()
                .map(visitorMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VisitorResponse getByPassportNumber(String passportNumber) {
        return visitorRepository.findByPassportNumberHash(blindIndexService.hmac(passportNumber))
                .map(visitorMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Visitor not found: " + passportNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VisitorResponse> list(Pageable pageable) {
        return visitorRepository.findAll(pageable).map(visitorMapper::toResponse);
    }

    @Override
    public VisitorResponse update(UUID id, VisitorRequest request) {
        Visitor visitor = getEntityById(id);
        policyService.getEntityById(request.policyId());
        validateCoverPeriod(request);
        String passportNumberHash = blindIndexService.hmac(request.passportNumber());
        if (visitorRepository.existsByPassportNumberHashAndIdNot(passportNumberHash, id)) {
            throw new IllegalStateException(
                    "Visitor already exists with passport number: " + request.passportNumber());
        }
        visitorMapper.updateEntity(visitor, request);
        visitor.setPassportNumberHash(passportNumberHash);
        return visitorMapper.toResponse(visitor);
    }

    @Override
    public void delete(UUID id) {
        Visitor visitor = getEntityById(id);
        visitorRepository.delete(visitor);
        eventPublisher.publishEvent(new VisitorDeletedEvent(visitor.getId(), visitor.getPolicyId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Visitor getEntityById(UUID id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Visitor getEntityByPassportNumber(String passportNumber) {
        return visitorRepository.findByPassportNumberHash(blindIndexService.hmac(passportNumber))
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", passportNumber));
    }

    @Override
    public VisitorResponse updateVisitorStatus(UUID id, VisitorStatusUpdate visitorStatusUpdate) {
        return applyStatusUpdate(getEntityById(id), visitorStatusUpdate);
    }

    @Override
    public VisitorResponse updateVisitorStatusByPassportNumber(String passportNumber,
                                                               VisitorStatusUpdate visitorStatusUpdate) {
        return applyStatusUpdate(getEntityByPassportNumber(passportNumber), visitorStatusUpdate);
    }

    @Override
    public VisitorResponse updateEntryExitByPassportNumber(String passportNumber,
                                                            VisitorEntryExitUpdate entryExitUpdate) {
        boolean hasEntry = entryExitUpdate.entryTimestamp() != null;
        boolean hasExit = entryExitUpdate.exitTimestamp() != null;
        if (hasEntry == hasExit) {
            throw new IllegalArgumentException(
                    "Exactly one of entryTimestamp or exitTimestamp must be provided");
        }
        Visitor visitor = getEntityByPassportNumber(passportNumber);
        if (hasEntry) {
            visitor.setEntryTimestamp(entryExitUpdate.entryTimestamp());
        } else {
            visitor.setExitTimestamp(entryExitUpdate.exitTimestamp());
        }
        return visitorMapper.toResponse(visitor);
    }

    private void validateCoverPeriod(VisitorRequest request) {
        if (request.dateOut().isBefore(request.dateIn())) {
            throw new IllegalArgumentException("Date out must not be before date in");
        }
        long days = ChronoUnit.DAYS.between(request.dateIn(), request.dateOut()) + 1;
        if (days < MIN_COVER_DAYS || days > MAX_COVER_DAYS) {
            throw new IllegalArgumentException(
                    "Travel period of %d day(s) is not valid (must be between %d and %d days)"
                            .formatted(days, MIN_COVER_DAYS, MAX_COVER_DAYS));
        }
    }

    /**
     * Validates that the policy's backing insurer has available policy tokens.
     * Prevents visitor creation if the insurer has exhausted their quota.
     *
     * @param policy the policy to validate
     * @throws IllegalStateException if the backing insurer has no available policies
     */
    private void validatePolicyQuota(Policy policy) {
        Insurer insurer = insurerRepository.findById(policy.getInsurerId())
                .orElseThrow(() -> new IllegalStateException(
                        "Insurer not found: " + policy.getInsurerId()));

        if (insurer.getPolicyToken() == null || insurer.getPolicyToken() <= 0) {
            throw new IllegalStateException(
                    "Insurer '" + insurer.getName() + "' has no available policies left");
        }
    }

    private VisitorResponse applyStatusUpdate(Visitor visitor, VisitorStatusUpdate visitorStatusUpdate) {
        VisitorStatus current = visitor.getVisitorStatus();
        VisitorStatus target = visitorStatusUpdate.visitorStatus();
        if (!current.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot change visitor status from " + current + " to " + target);
        }
        visitor.setVisitorStatus(target);
        eventPublisher.publishEvent(new VisitorStatusChangedEvent(visitor.getId(), target));
        return visitorMapper.toResponse(visitor);
    }
}
