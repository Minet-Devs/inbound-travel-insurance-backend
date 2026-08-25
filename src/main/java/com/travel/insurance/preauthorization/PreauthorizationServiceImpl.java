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
import com.travel.insurance.medicalservice.MedicalServiceService;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.policy.PolicyStatus;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationItemRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final PreauthorizationEnhancementRepository preauthorizationEnhancementRepository;
    private final PreauthorizationItemRepository preauthorizationItemRepository;
    private final PreauthorizationMapper preauthorizationMapper;
    private final PolicyService policyService;
    private final BenefitService benefitService;
    private final VisitorService visitorService;
    private final Icd11CodeService icd11CodeService;
    private final ServiceProviderService serviceProviderService;
    private final MedicalServiceService medicalServiceService;
    private final EventPublisher eventPublisher;

    @Override
    public PreauthorizationResponse create(PreauthorizationRequest request) {
        Policy policy = validatePolicyActive(request.policyId());
        validateVisitorExists(request.visitorId());
        validateBenefitExists(request.benefitId());
        icd11CodeService.getEntityById(request.icd11CodeId());
        serviceProviderService.getById(request.serviceProviderId());
        if (request.medicalServiceId() != null) {
            medicalServiceService.getById(request.medicalServiceId());
        }
        Preauthorization preauthorization = preauthorizationMapper.toEntity(request);
        preauthorization.setInsurerId(policy.getInsurerId());
        Preauthorization saved = preauthorizationRepository.save(preauthorization);
        PreauthorizationEnhancement enhancement = preauthorizationEnhancementRepository.save(
                preauthorizationMapper.toEnhancement(saved.getId(), request));
        saveItems(enhancement.getId(), request.preauthorizationItems());
        return enrich(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PreauthorizationResponse getById(UUID id) {
        return enrich(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PreauthorizationResponse> list(UUID insurerId, Pageable pageable) {
        return findScoped(insurerId, pageable).map(this::enrich);
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

    private void saveItems(UUID enhancementId, List<PreauthorizationItemRequest> itemRequests) {
        List<PreauthorizationItem> items = Optional.ofNullable(itemRequests).orElse(List.of()).stream()
                .map(itemRequest -> preauthorizationMapper.toItem(itemRequest, enhancementId))
                .toList();
        preauthorizationItemRepository.saveAll(items);
    }

    public void markPreAuthorizationConvertedToClaim(UUID id) {
        Preauthorization preauthorization = getEntityById(id);

        if (preauthorization != null && preauthorization.getConvertedToClaim().equals(false) && preauthorization.getStatus() == PreauthorizationStatus.APPROVED) {
            preauthorization.setConvertedToClaim(true);

            preauthorizationRepository.save(preauthorization);
        }
    }

    private PreauthorizationResponse enrich(Preauthorization preauthorization) {
        policyService.getEntityById(preauthorization.getPolicyId());
        Visitor visitor = preauthorization.getVisitorId() != null
                ? visitorService.getEntityById(preauthorization.getVisitorId())
                : null;
        Icd11Code icd11Code = preauthorization.getIcd11CodeId() != null
                ? icd11CodeService.getEntityById(preauthorization.getIcd11CodeId())
                : null;
        Benefit benefit = benefitService.getEntityById(preauthorization.getBenefitId());
        var serviceProvider = serviceProviderService.getById(preauthorization.getServiceProviderId());

        Optional<PreauthorizationEnhancement> enhancement =
                preauthorizationEnhancementRepository.findByPreauthorizationId(preauthorization.getId());
        UUID medicalServiceId = enhancement.map(PreauthorizationEnhancement::getMedicalServiceId).orElse(null);
        String medicalServiceName = medicalServiceId != null
                ? medicalServiceService.getById(medicalServiceId).name()
                : null;
        List<PreauthorizationItem> items = enhancement
                .map(e -> preauthorizationItemRepository.findAllByEnhancementId(e.getId()))
                .orElse(List.of());

        return preauthorizationMapper.toResponse(
                preauthorization, visitor, icd11Code, benefit, serviceProvider,
                medicalServiceId, medicalServiceName, items);
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

    private Policy validatePolicyActive(UUID policyId) {
        Policy policy = policyService.getEntityById(policyId);
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new IllegalStateException("Policy is not active: " + policyId);
        }
        return policy;
    }

    private void validateBenefitExists(UUID benefitId) {
        benefitService.getEntityById(benefitId);
    }

    private void validateVisitorExists(UUID visitorId) {
        visitorService.getEntityById(visitorId);
    }

    private Page<Preauthorization> findScoped(UUID insurerId, Pageable pageable) {
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user != null && "PROVIDER_USER".equals(user.role())) {
            UUID serviceProviderId = serviceProviderService.findIdByOrganizationId(user.organizationId())
                    .orElse(null);
            if (serviceProviderId == null) {
                return Page.empty(pageable);
            }
            return insurerId == null
                    ? preauthorizationRepository.findAllByServiceProviderId(serviceProviderId, pageable)
                    : preauthorizationRepository.findAllByServiceProviderIdAndInsurerId(
                            serviceProviderId, insurerId, pageable);
        }
        return insurerId == null
                ? preauthorizationRepository.findAll(pageable)
                : preauthorizationRepository.findAllByInsurerId(insurerId, pageable);
    }

    private void publishDecided(Preauthorization preauthorization) {
        eventPublisher.publish(RabbitConfig.PREAUTHORIZATION_DECIDED_KEY, Map.of(
                "preauthorizationId", preauthorization.getId().toString(),
                "policyId", preauthorization.getPolicyId().toString(),
                "status", preauthorization.getStatus().name()));
    }
}