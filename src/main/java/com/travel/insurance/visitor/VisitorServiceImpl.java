package com.travel.insurance.visitor;

import com.travel.insurance.common.crypto.BlindIndexService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerRepository;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.visitor.dto.AgeGroupVisitorCount;
import com.travel.insurance.visitor.dto.InsurerVisitorCount;
import com.travel.insurance.visitor.dto.VisitorEntryExitUpdate;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import com.travel.insurance.visitor.dto.VisitorStatusUpdate;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final CertificateSerialNumberGenerator certificateSerialNumberGenerator;

    @Override
    public VisitorResponse create(VisitorRequest request) {
        Policy policy = policyService.getEntityById(request.policyId());
        validateCoverPeriod(request);
        Insurer insurer = requireInsurerWithQuota(policy);
        String passportNumberHash = blindIndexService.hmac(request.passportNumber());
        if (visitorRepository.existsByPassportNumberHash(passportNumberHash)) {
            throw new IllegalStateException(
                    "Visitor already exists with passport number: " + request.passportNumber());
        }
        Visitor visitor = visitorMapper.toEntity(request);
        visitor.setInsurerId(policy.getInsurerId());
        visitor.setPassportNumberHash(passportNumberHash);
        visitor.setEmailHash(blindIndexService.hmac(request.email()));
        if (visitor.getVisitorStatus() == VisitorStatus.ACTIVE) {
            visitor.setCertificateSerialNumber(certificateSerialNumberGenerator.next(insurer.getName()));
        }
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
    public List<InsurerVisitorCount> countByInsurer() {
        Map<UUID, Long> totals = new HashMap<>();
        visitorRepository.countVisitorsGroupedByInsurer()
                .forEach(row -> totals.put(row.getInsurerId(), row.getTotal()));
        return insurerRepository.findAll().stream()
                .map(insurer -> new InsurerVisitorCount(
                        insurer.getId(), insurer.getName(), totals.getOrDefault(insurer.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgeGroupVisitorCount> countByAgeGroup() {
        // date_of_birth is encrypted at rest, so it cannot be bucketed in SQL.
        LocalDate today = LocalDate.now();
        long infants = 0;
        long minors = 0;
        long adults = 0;
        for (Visitor visitor : visitorRepository.findAll()) {
            if (visitor.getDateOfBirth() == null) {
                continue;
            }
            int age = Period.between(visitor.getDateOfBirth(), today).getYears();
            if (age < 3) {
                infants++;
            } else if (age < 18) {
                minors++;
            } else {
                adults++;
            }
        }
        return List.of(
                new AgeGroupVisitorCount("0-2", infants),
                new AgeGroupVisitorCount("3-17", minors),
                new AgeGroupVisitorCount("18+", adults));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VisitorResponse> list(UUID insurerId, Pageable pageable) {
        Page<Visitor> visitors = insurerId == null
                ? visitorRepository.findAll(pageable)
                : visitorRepository.findByInsurerId(insurerId, pageable);
        return visitors.map(visitorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorResponse> listForExport(UUID insurerId, VisitorStatus status,
                                               LocalDate dateFrom, LocalDate dateTo) {
        Specification<Visitor> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (insurerId != null) {
                predicates.add(cb.equal(root.get("insurerId"), insurerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("visitorStatus"), status));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateIn"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateIn"), dateTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return visitorRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateIn")).stream()
                .map(visitorMapper::toResponse)
                .toList();
    }

    @Override
    public VisitorResponse update(UUID id, VisitorRequest request) {
        Visitor visitor = getEntityById(id);
        Policy policy = policyService.getEntityById(request.policyId());
        validateCoverPeriod(request);
        String passportNumberHash = blindIndexService.hmac(request.passportNumber());
        if (visitorRepository.existsByPassportNumberHashAndIdNot(passportNumberHash, id)) {
            throw new IllegalStateException(
                    "Visitor already exists with passport number: " + request.passportNumber());
        }
        visitorMapper.updateEntity(visitor, request);
        visitor.setInsurerId(policy.getInsurerId());
        visitor.setPassportNumberHash(passportNumberHash);
        visitor.setEmailHash(blindIndexService.hmac(request.email()));
        return visitorMapper.toResponse(visitor);
    }

    @Override
    public void markActivationEmailSent(UUID id) {
        Visitor visitor = getEntityById(id);
        visitor.setActivationEmailSentAt(Instant.now());
        visitorRepository.save(visitor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findIdsAwaitingActivationEmail(Instant createdAfter, Instant createdBefore, int limit) {
        return visitorRepository
                .findByVisitorStatusAndActivationEmailSentAtIsNullAndCreatedDateBetween(
                        VisitorStatus.ACTIVE, createdAfter, createdBefore,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdDate")))
                .stream().map(Visitor::getId).toList();
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
    @Transactional(readOnly = true)
    public Optional<Visitor> findByEmail(String email) {
        return visitorRepository.findFirstByEmailHashOrderByCreatedDateDesc(blindIndexService.hmac(email));
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
     * Validates that the policy's backing insurer has available policy tokens
     * and returns it, so callers don't have to re-fetch it.
     *
     * @param policy the policy to validate
     * @throws IllegalStateException if the backing insurer has no available policies
     */
    private Insurer requireInsurerWithQuota(Policy policy) {
        Insurer insurer = insurerRepository.findById(policy.getInsurerId())
                .orElseThrow(() -> new IllegalStateException(
                        "Insurer not found: " + policy.getInsurerId()));

        if (insurer.getPolicyToken() == null || insurer.getPolicyToken() <= 0) {
            throw new IllegalStateException(
                    "Insurer '" + insurer.getName() + "' has no available policies left");
        }
        return insurer;
    }

    private VisitorResponse applyStatusUpdate(Visitor visitor, VisitorStatusUpdate visitorStatusUpdate) {
        VisitorStatus current = visitor.getVisitorStatus();
        VisitorStatus target = visitorStatusUpdate.visitorStatus();
        if (!current.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot change visitor status from " + current + " to " + target);
        }
        visitor.setVisitorStatus(target);
        if (target == VisitorStatus.ACTIVE && visitor.getCertificateSerialNumber() == null) {
            Insurer insurer = insurerRepository.findById(visitor.getInsurerId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Insurer not found: " + visitor.getInsurerId()));
            visitor.setCertificateSerialNumber(certificateSerialNumberGenerator.next(insurer.getName()));
        }
        eventPublisher.publishEvent(new VisitorStatusChangedEvent(visitor.getId(), target));
        return visitorMapper.toResponse(visitor);
    }
}
