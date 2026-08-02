package com.travel.insurance.claim;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.dto.ClaimDecisionRequest;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.common.util.SecurityUtils;
import com.travel.insurance.config.RabbitConfig;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimServiceImpl implements ClaimService {

    private static final Set<ClaimStatus> DECISION_STATUSES = EnumSet.of(
            ClaimStatus.UNDER_REVIEW,
            ClaimStatus.APPROVED,
            ClaimStatus.PARTIALLY_APPROVED,
            ClaimStatus.REJECTED);

    private static final Set<ClaimStatus> OPEN_STATUSES = EnumSet.of(
            ClaimStatus.SUBMITTED,
            ClaimStatus.UNDER_REVIEW);

    private final ClaimRepository claimRepository;
    private final ClaimMapper claimMapper;
    private final PolicyService policyService;
    private final BenefitService benefitService;
    private final EventPublisher eventPublisher;

    @Override
    public ClaimResponse create(ClaimRequest request) {
        validateReferences(request);
        Claim claim = claimRepository.save(claimMapper.toEntity(request));
        return claimMapper.toResponse(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimResponse getById(UUID id) {
        return claimMapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimResponse> list(Pageable pageable) {
        return findScoped(pageable).map(claimMapper::toResponse);
    }

    @Override
    public ClaimResponse decide(UUID id, ClaimDecisionRequest request) {
        Claim claim = getEntity(id);
        if (!OPEN_STATUSES.contains(claim.getStatus())) {
            throw new IllegalStateException("Claim is not open for decisions: " + claim.getStatus());
        }
        if (!DECISION_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("Decision status must be one of " + DECISION_STATUSES);
        }
        applyDecision(claim, request);
        Claim saved = claimRepository.save(claim);
        publishIfApproved(saved);
        return claimMapper.toResponse(saved);
    }

    @Override
    public ClaimResponse markPaid(UUID id) {
        Claim claim = getEntity(id);
        if (claim.getStatus() != ClaimStatus.APPROVED && claim.getStatus() != ClaimStatus.PARTIALLY_APPROVED) {
            throw new IllegalStateException("Only approved claims can be marked as paid");
        }
        claim.setStatus(ClaimStatus.PAID);
        return claimMapper.toResponse(claimRepository.save(claim));
    }

    @Override
    public void delete(UUID id) {
        claimRepository.delete(getEntity(id));
    }

    private void applyDecision(Claim claim, ClaimDecisionRequest request) {
        claim.setStatus(request.status());
        claim.setDecisionReason(request.reason());
        if (request.status() == ClaimStatus.REJECTED) {
            claim.setApprovedAmount(BigDecimal.ZERO);
            return;
        }
        if (request.status() == ClaimStatus.UNDER_REVIEW) {
            return;
        }
        BigDecimal approved = request.approvedAmount() != null
                ? request.approvedAmount()
                : claim.getClaimedAmount();
        if (approved.compareTo(claim.getClaimedAmount()) > 0) {
            throw new IllegalArgumentException("Approved amount cannot exceed claimed amount");
        }
        claim.setApprovedAmount(approved);
        if (claim.getPreauthorizationId() == null) {
            benefitService.drawDown(claim.getBenefitId(), approved);
        }
    }

    private void validateReferences(ClaimRequest request) {
        policyService.getEntityById(request.policyId());
        if (!benefitService.getEntityById(request.benefitId()).getPolicyId().equals(request.policyId())) {
            throw new IllegalArgumentException("Benefit does not belong to the given policy");
        }
    }

    private Page<Claim> findScoped(Pageable pageable) {
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user != null && user.roles().contains("PROVIDER_USER")) {
            return claimRepository.findAllByServiceProviderId(user.organizationId(), pageable);
        }
        return claimRepository.findAll(pageable);
    }

    private void publishIfApproved(Claim claim) {
        if (claim.getStatus() != ClaimStatus.APPROVED && claim.getStatus() != ClaimStatus.PARTIALLY_APPROVED) {
            return;
        }
        Policy policy = policyService.getEntityById(claim.getPolicyId());
        eventPublisher.publish(RabbitConfig.CLAIM_APPROVED_KEY, Map.of(
                "claimId", claim.getId().toString(),
                "policyId", claim.getPolicyId().toString(),
                "insurerId", policy.getInsurerId().toString(),
                "approvedAmount", claim.getApprovedAmount().toPlainString(),
                "status", claim.getStatus().name()));
    }

    private Claim getEntity(UUID id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", id));
    }
}
