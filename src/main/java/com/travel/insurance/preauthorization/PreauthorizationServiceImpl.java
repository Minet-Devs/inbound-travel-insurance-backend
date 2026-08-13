package com.travel.insurance.preauthorization;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.common.util.SecurityUtils;
import com.travel.insurance.config.RabbitConfig;
import com.travel.insurance.icd11.Icd11Code;
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.policy.PolicyStatus;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
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
public class PreauthorizationServiceImpl implements PreauthorizationService {

    private static final Set<PreauthorizationStatus> DECISION_STATUSES = EnumSet.of(
            PreauthorizationStatus.APPROVED,
            PreauthorizationStatus.PARTIALLY_APPROVED,
            PreauthorizationStatus.REJECTED);

    private final PreauthorizationRepository preauthorizationRepository;
    private final PreauthorizationMapper preauthorizationMapper;
    private final PolicyService policyService;
    private final BenefitService benefitService;
    private final VisitorService visitorService;
    private final Icd11CodeService icd11CodeService;
    private final ServiceProviderService serviceProviderService;
    private final EventPublisher eventPublisher;

    @Override
    public PreauthorizationResponse create(PreauthorizationRequest request) {
        validatePolicyActive(request.policyId());
        validateVisitorExists(request.visitorId());
        validateBenefitExists(request.benefitId());
        icd11CodeService.getEntityById(request.icd11CodeId());
        serviceProviderService.getById(request.serviceProviderId());
        Preauthorization saved = preauthorizationRepository.save(preauthorizationMapper.toEntity(request));
        return enrich(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PreauthorizationResponse getById(UUID id) {
        return enrich(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PreauthorizationResponse> list(Pageable pageable) {
        return findScoped(pageable).map(this::enrich);
    }

    @Override
    public PreauthorizationResponse decide(UUID id, PreauthorizationDecisionRequest request) {
        Preauthorization preauthorization = getEntityById(id);
        if (preauthorization.getStatus() != PreauthorizationStatus.PENDING) {
            throw new IllegalStateException("Pre-authorization has already been decided");
        }
        if (!DECISION_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("Decision status must be one of " + DECISION_STATUSES);
        }
        applyDecision(preauthorization, request);
        Preauthorization saved = preauthorizationRepository.save(preauthorization);
        publishDecided(saved);
        return enrich(saved);
    }

    @Override
    public void delete(UUID id) {
        preauthorizationRepository.delete(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Preauthorization getEntityById(UUID id) {
        return preauthorizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preauthorization", id));
    }

    private PreauthorizationResponse enrich(Preauthorization preauthorization) {
        Policy policy = policyService.getEntityById(preauthorization.getPolicyId());
        Visitor visitor = preauthorization.getVisitorId() != null
                ? visitorService.getEntityById(preauthorization.getVisitorId())
                : null;
        Icd11Code icd11Code = preauthorization.getIcd11CodeId() != null
                ? icd11CodeService.getEntityById(preauthorization.getIcd11CodeId())
                : null;
        Benefit benefit = benefitService.getEntityById(preauthorization.getBenefitId());
        var serviceProvider = serviceProviderService.getById(preauthorization.getServiceProviderId());
        return preauthorizationMapper.toResponse(preauthorization, policy, visitor, icd11Code, benefit, serviceProvider);
    }

    private void applyDecision(Preauthorization preauthorization, PreauthorizationDecisionRequest request) {
        preauthorization.setStatus(request.status());
        preauthorization.setDecisionReason(request.reason());
        if (request.status() == PreauthorizationStatus.REJECTED) {
            preauthorization.setApprovedAmount(BigDecimal.ZERO);
            return;
        }
        BigDecimal approved = request.approvedAmount() != null
                ? request.approvedAmount()
                : preauthorization.getRequestedAmount();
        if (approved.compareTo(preauthorization.getRequestedAmount()) > 0) {
            throw new IllegalArgumentException("Approved amount cannot exceed requested amount");
        }
        preauthorization.setApprovedAmount(approved);
    }

    private void validatePolicyActive(UUID policyId) {
        Policy policy = policyService.getEntityById(policyId);
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new IllegalStateException("Policy is not active: " + policyId);
        }
    }

    private void validateBenefitExists(UUID benefitId) {
        benefitService.getEntityById(benefitId);
    }

    private void validateVisitorExists(UUID visitorId) {
        visitorService.getEntityById(visitorId);
    }

    private Page<Preauthorization> findScoped(Pageable pageable) {
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user != null && user.roles().contains("PROVIDER_USER")) {
            return preauthorizationRepository.findAllByServiceProviderId(user.organizationId(), pageable);
        }
        return preauthorizationRepository.findAll(pageable);
    }

    private void publishDecided(Preauthorization preauthorization) {
        eventPublisher.publish(RabbitConfig.PREAUTHORIZATION_DECIDED_KEY, Map.of(
                "preauthorizationId", preauthorization.getId().toString(),
                "policyId", preauthorization.getPolicyId().toString(),
                "status", preauthorization.getStatus().name()));
    }
}