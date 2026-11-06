package com.travel.insurance.preauthorization;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.common.exception.ExchangeRateUnavailableException;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.common.service.CurrencyConversionService;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.common.util.SecurityUtils;
import com.travel.insurance.config.RabbitConfig;
import com.travel.insurance.icd11.Icd11Code;
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.medicalservice.MedicalServiceService;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.policy.PolicyStatus;
import com.travel.insurance.insurer.dto.InsurerResponse;
import com.travel.insurance.preauthorization.dto.InsurerPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.OfflineApproval;
import com.travel.insurance.preauthorization.dto.PreauthorizationDashboardSummary;
import com.travel.insurance.preauthorization.dto.ServiceProviderPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationItemRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationUpdateRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PreauthorizationServiceImpl implements PreauthorizationService {

    private static final Set<PreauthorizationStatus> DECISION_STATUSES = EnumSet.of(
            PreauthorizationStatus.APPROVED,
            PreauthorizationStatus.PARTIALLY_APPROVED,
            PreauthorizationStatus.REJECTED);

    private static final Set<PreauthorizationStatus> GRANTED_STATUSES =
            EnumSet.of(PreauthorizationStatus.APPROVED, PreauthorizationStatus.PARTIALLY_APPROVED);
    private static final int DEFAULT_TOP_SERVICE_PROVIDERS = 10;
    private static final String OFFLINE_APPROVAL_REASON = "Offline approval (bulk upload)";

    private final PreauthorizationRepository preauthorizationRepository;
    private final PreauthorizationEnhancementRepository preauthorizationEnhancementRepository;
    private final PreauthorizationItemRepository preauthorizationItemRepository;
    private final PreauthorizationMapper preauthorizationMapper;
    private final PolicyService policyService;
    private final BenefitService benefitService;
    private final VisitorService visitorService;
    private final Icd11CodeService icd11CodeService;
    private final ServiceProviderService serviceProviderService;
    private final InsurerService insurerService;
    private final MedicalServiceService medicalServiceService;
    private final EventPublisher eventPublisher;
    private final CurrencyConversionService currencyConversionService;

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
        preauthorization.setExchangeRate(lookupKesToUsdRate());
        Preauthorization saved = preauthorizationRepository.save(preauthorization);
        PreauthorizationEnhancement enhancement = preauthorizationEnhancementRepository.save(
                preauthorizationMapper.toEnhancement(saved.getId(), request));
        saveItems(enhancement.getId(), request.preauthorizationItems());
        return enrich(saved);
    }

    @Override
    public PreauthorizationResponse update(UUID id, PreauthorizationUpdateRequest request) {
        Preauthorization preauthorization = getEntityById(id);
        assertVisibleToCurrentUser(preauthorization);
        Policy policy = validatePolicyActive(request.policyId());
        validateVisitorExists(request.visitorId());
        validateBenefitExists(request.benefitId());
        if (request.icd11CodeId() != null) {
            icd11CodeService.getEntityById(request.icd11CodeId());
        }
        serviceProviderService.getById(request.serviceProviderId());
        if (request.medicalServiceId() != null) {
            medicalServiceService.getById(request.medicalServiceId());
        }
        preauthorization.setPolicyId(request.policyId());
        preauthorization.setInsurerId(policy.getInsurerId());
        preauthorization.setVisitorId(request.visitorId());
        preauthorization.setIcd11CodeId(request.icd11CodeId());
        preauthorization.setBenefitId(request.benefitId());
        preauthorization.setServiceProviderId(request.serviceProviderId());
        preauthorization.setRequestedAmount(request.requestedAmount());
        preauthorization.setServiceDescription(request.serviceDescription());
        preauthorization.setVisitType(request.visitType());
        applyOptionalUpdates(preauthorization, request);
        Preauthorization saved = preauthorizationRepository.save(preauthorization);

        PreauthorizationEnhancement enhancement = preauthorizationEnhancementRepository
                .findByPreauthorizationId(id)
                .orElseGet(() -> {
                    PreauthorizationEnhancement created = new PreauthorizationEnhancement();
                    created.setPreauthorizationId(id);
                    return created;
                });
        enhancement.setMedicalServiceId(request.medicalServiceId());
        enhancement.setRequestedAmount(request.requestedAmount());
        PreauthorizationEnhancement savedEnhancement = preauthorizationEnhancementRepository.save(enhancement);
        preauthorizationItemRepository.deleteAll(
                preauthorizationItemRepository.findAllByEnhancementId(savedEnhancement.getId()));
        saveItems(savedEnhancement.getId(), request.preauthorizationItems());
        return enrich(saved);
    }

    /** Optional fields of an update: omitted (null) leaves the stored value untouched. */
    private void applyOptionalUpdates(Preauthorization preauthorization, PreauthorizationUpdateRequest request) {
        if (request.approvedAmount() != null) {
            if (request.approvedAmount().compareTo(preauthorization.getRequestedAmount()) > 0) {
                throw new IllegalArgumentException("Approved amount cannot exceed requested amount");
            }
            preauthorization.setApprovedAmount(request.approvedAmount());
        }
        if (request.exchangeRate() != null) {
            preauthorization.setExchangeRate(request.exchangeRate());
        }
        if (request.approvedAmountInDollars() != null) {
            preauthorization.setApprovedAmountInDollars(request.approvedAmountInDollars());
        } else if (request.approvedAmount() != null || request.exchangeRate() != null) {
            preauthorization.setApprovedAmountInDollars(toDollars(preauthorization));
        }
        if (request.status() != null) {
            preauthorization.setStatus(request.status());
        }
        if (request.decisionReason() != null) {
            preauthorization.setDecisionReason(request.decisionReason());
        }
        if (request.decidedBy() != null) {
            preauthorization.setDecidedBy(request.decidedBy());
        }
        if (request.decidedAt() != null) {
            preauthorization.setDecidedAt(request.decidedAt());
        }
        if (request.convertedToClaim() != null) {
            preauthorization.setConvertedToClaim(request.convertedToClaim());
        }
        if (request.treatmentDate() != null) {
            preauthorization.setTreatmentDate(request.treatmentDate());
        }
        if (request.dischargeDate() != null) {
            preauthorization.setDischargeDate(request.dischargeDate());
        }
    }

    @Override
    public UUID createOfflineApproval(OfflineApproval approval) {
        Visitor visitor = visitorService.getEntityById(approval.visitorId());
        Policy policy = validatePolicyActive(visitor.getPolicyId());
        validateBenefitExists(approval.benefitId());
        serviceProviderService.getById(approval.serviceProviderId());
        if (preauthorizationRepository
                .existsByVisitorIdAndServiceProviderIdAndBenefitIdAndTreatmentDateAndRequestedAmount(
                        approval.visitorId(), approval.serviceProviderId(), approval.benefitId(),
                        approval.treatmentDate(), approval.amount())) {
            throw new IllegalStateException("A matching pre-authorization already exists");
        }
        Preauthorization preauthorization = new Preauthorization();
        preauthorization.setPolicyId(policy.getId());
        preauthorization.setInsurerId(policy.getInsurerId());
        preauthorization.setVisitorId(approval.visitorId());
        preauthorization.setBenefitId(approval.benefitId());
        preauthorization.setServiceProviderId(approval.serviceProviderId());
        preauthorization.setRequestedAmount(approval.amount());
        preauthorization.setApprovedAmount(approval.amount());
        preauthorization.setVisitType(approval.visitType());
        preauthorization.setTreatmentDate(approval.treatmentDate());
        preauthorization.setDischargeDate(approval.dischargeDate());
        preauthorization.setExchangeRate(lookupKesToUsdRate());
        preauthorization.setApprovedAmountInDollars(toDollars(preauthorization));
        preauthorization.setStatus(PreauthorizationStatus.APPROVED);
        preauthorization.setDecisionReason(OFFLINE_APPROVAL_REASON);
        return preauthorizationRepository.save(preauthorization).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PreauthorizationResponse getById(UUID id) {
        Preauthorization preauthorization = getEntityById(id);
        assertVisibleToCurrentUser(preauthorization);
        return enrich(preauthorization);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PreauthorizationResponse> list(UUID insurerId, Pageable pageable) {
        return findScoped(insurerId, pageable).map(this::enrich);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PreauthorizationResponse> listByVisitor(UUID visitorId, Pageable pageable) {
        return preauthorizationRepository.findAllByVisitorId(visitorId, pageable).map(this::enrich);
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
    @Transactional(readOnly = true)
    public PreauthorizationDashboardSummary dashboardSummary(LocalDate dateFrom, LocalDate dateTo) {
        DashboardScope scope = resolveDashboardScope();
        if (scope.empty()) {
            return new PreauthorizationDashboardSummary(0, 0, 0, 0);
        }
        Map<PreauthorizationStatus, Long> counts = new EnumMap<>(PreauthorizationStatus.class);
        for (PreauthorizationStatusCount row : preauthorizationRepository.countByStatus(
                scope.insurerId(), scope.serviceProviderId(), startOf(dateFrom), endOf(dateTo))) {
            counts.put(row.getStatus(), row.getStatusCount());
        }
        return new PreauthorizationDashboardSummary(
                counts.values().stream().mapToLong(Long::longValue).sum(),
                counts.getOrDefault(PreauthorizationStatus.APPROVED, 0L),
                counts.getOrDefault(PreauthorizationStatus.REJECTED, 0L),
                counts.getOrDefault(PreauthorizationStatus.PARTIALLY_APPROVED, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsurerPreauthorizationDistribution> distributionByInsurer(LocalDate dateFrom, LocalDate dateTo) {
        DashboardScope scope = resolveDashboardScope();
        if (scope.empty()) {
            return List.of();
        }
        Map<UUID, PreauthorizationDashboardTotal> totals = new HashMap<>();
        for (PreauthorizationDashboardTotal row : preauthorizationRepository.summarizeByInsurer(
                GRANTED_STATUSES, scope.insurerId(), scope.serviceProviderId(), startOf(dateFrom), endOf(dateTo))) {
            totals.put(row.getGroupId(), row);
        }
        List<InsurerPreauthorizationDistribution> result = new ArrayList<>();
        for (InsurerResponse insurer : insurerService.listAll()) {
            if (scope.insurerId() != null && !scope.insurerId().equals(insurer.id())) {
                continue;
            }
            PreauthorizationDashboardTotal total = totals.get(insurer.id());
            result.add(new InsurerPreauthorizationDistribution(insurer.id(), insurer.name(),
                    total != null ? total.getPreauthorizationCount() : 0,
                    total != null ? total.getApprovedAmount() : BigDecimal.ZERO));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceProviderPreauthorizationDistribution> topServiceProviders(
            LocalDate dateFrom, LocalDate dateTo, Integer limit) {
        int top = limit != null ? limit : DEFAULT_TOP_SERVICE_PROVIDERS;
        if (top < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        DashboardScope scope = resolveDashboardScope();
        if (scope.empty()) {
            return List.of();
        }
        List<PreauthorizationDashboardTotal> totals = preauthorizationRepository.summarizeByServiceProvider(
                GRANTED_STATUSES, scope.insurerId(), scope.serviceProviderId(), startOf(dateFrom), endOf(dateTo));
        Map<UUID, String> names = serviceProviderService.namesByIds(
                totals.stream().map(PreauthorizationDashboardTotal::getGroupId).toList());
        return totals.stream()
                .sorted(Comparator.comparingLong(PreauthorizationDashboardTotal::getPreauthorizationCount).reversed()
                        .thenComparing(PreauthorizationDashboardTotal::getApprovedAmount, Comparator.reverseOrder()))
                .limit(top)
                .map(total -> new ServiceProviderPreauthorizationDistribution(total.getGroupId(),
                        names.get(total.getGroupId()), total.getPreauthorizationCount(), total.getApprovedAmount()))
                .toList();
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

    /** The rate is optional: an unavailable rate service must not block raising a pre-authorization. */
    private BigDecimal lookupKesToUsdRate() {
        try {
            return currencyConversionService.getExchangeRate("KES", "USD");
        } catch (ExchangeRateUnavailableException ex) {
            log.warn("KES->USD exchange rate unavailable; saving pre-authorization without it");
            return null;
        }
    }

    private void saveItems(UUID enhancementId, List<PreauthorizationItemRequest> itemRequests) {
        List<PreauthorizationItem> items = Optional.ofNullable(itemRequests).orElse(List.of()).stream()
                .map(itemRequest -> preauthorizationMapper.toItem(itemRequest, enhancementId))
                .toList();
        preauthorizationItemRepository.saveAll(items);
    }

    public void markPreAuthorizationConvertedToClaim(UUID id) {
        Preauthorization preauthorization = getEntityById(id);

        boolean approved = preauthorization.getStatus() == PreauthorizationStatus.APPROVED
                || preauthorization.getStatus() == PreauthorizationStatus.PARTIALLY_APPROVED;
        if (approved && !Boolean.TRUE.equals(preauthorization.getConvertedToClaim())) {
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
            preauthorization.setApprovedAmountInDollars(toDollars(preauthorization));
            return;
        }
        BigDecimal approved = request.approvedAmount() != null
                ? request.approvedAmount()
                : preauthorization.getRequestedAmount();
        if (approved.compareTo(preauthorization.getRequestedAmount()) > 0) {
            throw new IllegalArgumentException("Approved amount cannot exceed requested amount");
        }
        preauthorization.setApprovedAmount(approved);
        preauthorization.setApprovedAmountInDollars(toDollars(preauthorization));
    }

    /** approvedAmount * exchangeRate, or null when either is missing. */
    private BigDecimal toDollars(Preauthorization preauthorization) {
        if (preauthorization.getApprovedAmount() == null || preauthorization.getExchangeRate() == null) {
            return null;
        }
        return preauthorization.getApprovedAmount()
                .multiply(preauthorization.getExchangeRate())
                .setScale(2, RoundingMode.HALF_UP);
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

    /** Insurer and provider users are pinned to their own entity; an unresolvable entity means no data. */
    private DashboardScope resolveDashboardScope() {
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user != null && "PROVIDER_USER".equals(user.role())) {
            return serviceProviderService.findIdByOrganizationId(user.organizationId())
                    .map(id -> new DashboardScope(null, id, false))
                    .orElse(DashboardScope.NONE);
        }
        if (user != null && "INSURER_USER".equals(user.role())) {
            return insurerService.findIdByOrganizationId(user.organizationId())
                    .map(id -> new DashboardScope(id, null, false))
                    .orElse(DashboardScope.NONE);
        }
        return DashboardScope.ALL;
    }

    private static Instant startOf(LocalDate date) {
        return date != null ? date.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
    }

    private static Instant endOf(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
    }

    private record DashboardScope(UUID insurerId, UUID serviceProviderId, boolean empty) {
        static final DashboardScope ALL = new DashboardScope(null, null, false);
        static final DashboardScope NONE = new DashboardScope(null, null, true);
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
        if (user != null && "INSURER_USER".equals(user.role())) {
            // Insurer users only ever see their own insurer's pre-auths, whatever insurerId they pass.
            UUID ownInsurerId = insurerService.findIdByOrganizationId(user.organizationId()).orElse(null);
            if (ownInsurerId == null) {
                return Page.empty(pageable);
            }
            return preauthorizationRepository.findAllByInsurerId(ownInsurerId, pageable);
        }
        return insurerId == null
                ? preauthorizationRepository.findAll(pageable)
                : preauthorizationRepository.findAllByInsurerId(insurerId, pageable);
    }

    /**
     * Org-scoped users may only read their own organization's pre-auths: an insurer user those raised
     * against their insurer, a provider user those raised by their service provider.
     */
    private void assertVisibleToCurrentUser(Preauthorization preauthorization) {
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user == null) {
            return;
        }
        if ("INSURER_USER".equals(user.role())) {
            UUID insurerId = insurerService.findIdByOrganizationId(user.organizationId()).orElse(null);
            if (insurerId == null || !insurerId.equals(preauthorization.getInsurerId())) {
                throw new AccessDeniedException("Pre-authorization belongs to another insurer");
            }
        } else if ("PROVIDER_USER".equals(user.role())) {
            UUID serviceProviderId = serviceProviderService.findIdByOrganizationId(user.organizationId())
                    .orElse(null);
            if (serviceProviderId == null || !serviceProviderId.equals(preauthorization.getServiceProviderId())) {
                throw new AccessDeniedException("Pre-authorization belongs to another service provider");
            }
        }
    }

    private void publishDecided(Preauthorization preauthorization) {
        eventPublisher.publish(RabbitConfig.PREAUTHORIZATION_DECIDED_KEY, Map.of(
                "preauthorizationId", preauthorization.getId().toString(),
                "policyId", preauthorization.getPolicyId().toString(),
                "status", preauthorization.getStatus().name()));
    }
}