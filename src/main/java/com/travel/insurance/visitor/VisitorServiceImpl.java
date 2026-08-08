package com.travel.insurance.visitor;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.policy.PolicyType;
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

    private final VisitorRepository visitorRepository;
    private final VisitorMapper visitorMapper;
    private final PolicyService policyService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public VisitorResponse create(VisitorRequest request) {
        Policy policy = policyService.getEntityById(request.policyId());
        validateCoverPeriod(policy, request);
        if (visitorRepository.existsByPassportNumberIgnoreCase(request.passportNumber())) {
            throw new IllegalStateException(
                    "Visitor already exists with passport number: " + request.passportNumber());
        }
        Visitor visitor = visitorRepository.save(visitorMapper.toEntity(request));
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
        return visitorRepository.findByPassportNumberIgnoreCase(passportNumber)
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
        Policy policy = policyService.getEntityById(request.policyId());
        validateCoverPeriod(policy, request);
        if (visitorRepository.existsByPassportNumberIgnoreCaseAndIdNot(request.passportNumber(), id)) {
            throw new IllegalStateException(
                    "Visitor already exists with passport number: " + request.passportNumber());
        }
        visitorMapper.updateEntity(visitor, request);
        return visitorMapper.toResponse(visitor);
    }

    @Override
    public void delete(UUID id) {
        visitorRepository.delete(getEntityById(id));
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
        return visitorRepository.findByPassportNumberIgnoreCase(passportNumber)
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

    private void validateCoverPeriod(Policy policy, VisitorRequest request) {
        if (request.dateOut().isBefore(request.dateIn())) {
            throw new IllegalArgumentException("Date out must not be before date in");
        }
        long days = ChronoUnit.DAYS.between(request.dateIn(), request.dateOut()) + 1;
        PolicyType policyType = policy.getPolicyType();
        if (!policyType.isValidDuration(days)) {
            throw new IllegalArgumentException(
                    "Travel period of %d day(s) is not valid for policy type %s (must be between %d and %d days)"
                            .formatted(days, policyType, policyType.getMinDays(), policyType.getMaxDays()));
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
