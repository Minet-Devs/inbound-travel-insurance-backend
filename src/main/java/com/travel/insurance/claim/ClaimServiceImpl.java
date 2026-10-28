package com.travel.insurance.claim;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.dto.AttachInvoiceRequest;
import com.travel.insurance.claim.dto.ClaimDashboardSummary;
import com.travel.insurance.claim.dto.ClaimDecisionRequest;
import com.travel.insurance.claim.dto.ClaimExportRow;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.claim.dto.ConvertPreauthorizationRequest;
import com.travel.insurance.claim.dto.InsurerClaimDistribution;
import com.travel.insurance.claim.dto.ServiceProviderClaimDistribution;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.common.service.CurrencyConversionService;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.common.util.SecurityUtils;
import com.travel.insurance.config.RabbitConfig;
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.insurer.dto.InsurerResponse;
import com.travel.insurance.invoice.Invoice;
import com.travel.insurance.invoice.InvoiceService;
import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.preauthorization.Preauthorization;
import com.travel.insurance.preauthorization.PreauthorizationService;
import com.travel.insurance.preauthorization.PreauthorizationStatus;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import com.travel.insurance.procedure.ProcedureService;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.dto.VisitorResponse;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClaimServiceImpl implements ClaimService {

    private static final Set<ClaimStatus> DECISION_STATUSES = EnumSet.of(
            ClaimStatus.UNDER_REVIEW,
            ClaimStatus.APPROVED,
            ClaimStatus.PARTIALLY_APPROVED,
            ClaimStatus.REJECTED);

    private static final Set<PreauthorizationStatus> CONVERTIBLE_PREAUTH_STATUSES = EnumSet.of(
            PreauthorizationStatus.APPROVED,
            PreauthorizationStatus.PARTIALLY_APPROVED);

    private static final Set<ClaimStatus> OPEN_STATUSES = EnumSet.of(
            ClaimStatus.OPEN,
            ClaimStatus.SUBMITTED,
            ClaimStatus.UNDER_REVIEW);

    private static final String UNKNOWN_LABEL = "Unknown";

    private final ClaimRepository claimRepository;
    private final ClaimMapper claimMapper;
    private final PolicyService policyService;
    private final BenefitService benefitService;
    private final VisitorService visitorService;
    private final InsurerService insurerService;
    private final ServiceProviderService serviceProviderService;
    private final InvoiceService invoiceService;
    private final Icd11CodeService icd11CodeService;
    private final ProcedureService procedureService;
    private final CurrencyConversionService currencyConversionService;
    private final EventPublisher eventPublisher;
    private final PreauthorizationService preauthorizationService;

    @Override
    public ClaimResponse create(ClaimRequest request) {
        UUID insurerId = validateReferences(request);
        Claim claim = claimMapper.toEntity(request);
        if (request.preauthorizationId() != null && claim.getDiagnosisIds().isEmpty()) {
            Preauthorization preauth = preauthorizationService.getEntityById(request.preauthorizationId());
            if (preauth.getIcd11CodeId() != null) {
                claim.getDiagnosisIds().add(preauth.getIcd11CodeId());
            }
        }
        if (claim.getDiagnosisIds().isEmpty()) {
            throw new IllegalArgumentException("diagnosisIds is required when no preauthorization is provided");
        }
        if (claim.getProcedureIds().isEmpty()) {
            throw new IllegalArgumentException("procedureIds are required");
        }
        claim.setStatus(ClaimStatus.OPEN);
        claim.setInsurerId(insurerId);
        applyBaseConversion(claim);
        claim = claimRepository.save(claim);

        if (request.preauthorizationId() != null){
            attachClaimToPreAuthorization(claim.getId());
        }

        return toResponse(claim);
    }

    /**
     * Everything is copied from the pre-authorization; its items become the invoice's, 1:1.
     * Unlike {@link #create}, procedureIds are not required — the pre-auth never captured any.
     */
    @Override
    public ClaimResponse createFromPreauthorization(UUID preauthorizationId, ConvertPreauthorizationRequest request) {
        PreauthorizationResponse preauth = preauthorizationService.getById(preauthorizationId);
        if (!CONVERTIBLE_PREAUTH_STATUSES.contains(preauth.status())) {
            throw new IllegalStateException(
                    "Only approved pre-authorizations can be converted to a claim: " + preauth.status());
        }
        if (Boolean.TRUE.equals(preauth.convertedToClaim())) {
            throw new IllegalStateException("Pre-authorization has already been converted to a claim");
        }

        Claim claim = new Claim();
        claim.setPolicyId(preauth.policyId());
        claim.setInsurerId(preauth.insurerId());
        claim.setVisitorId(preauth.visitorId());
        claim.setBenefitId(preauth.benefitId());
        claim.setServiceProviderId(preauth.serviceProviderId());
        claim.setPreauthorizationId(preauth.id());
        claim.setClaimedAmount(preauth.approvedAmount() != null ? preauth.approvedAmount() : preauth.requestedAmount());
        claim.setDescription(preauth.serviceDescription());
        claim.getDiagnosisIds().add(preauth.icd11CodeId());
        claim.setStatus(ClaimStatus.OPEN);
        applyBaseConversion(claim);
        claim = claimRepository.save(claim);

        List<InvoiceItemRequest> items = preauth.preauthorizationItems().stream()
                .map(item -> new InvoiceItemRequest(preauth.medicalServiceId(), item.description(),
                        item.quantity(), item.unitPrice(), item.amount(), item.serviceDate()))
                .toList();
        BigDecimal invoiceTotal = items.isEmpty()
                ? claim.getClaimedAmount()
                : items.stream().map(InvoiceItemRequest::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        invoiceService.create(new InvoiceRequest(claim.getId(), request.invoiceNumber().trim(),
                request.issueDate(), claim.getCurrency(), invoiceTotal, items));

        preauthorizationService.markPreAuthorizationConvertedToClaim(preauth.id());
        return toResponse(claim);
    }

    @Override
    public ClaimResponse update(UUID id, ClaimRequest request) {
        Claim claim = getEntity(id);
        if (!OPEN_STATUSES.contains(claim.getStatus())) {
            throw new IllegalStateException("Claim is not open for updates: " + claim.getStatus());
        }
        UUID insurerId = validateReferences(request);
        claimMapper.updateEntity(claim, request);
        if (request.preauthorizationId() != null && claim.getDiagnosisIds().isEmpty()) {
            Preauthorization preauth = preauthorizationService.getEntityById(request.preauthorizationId());
            if (preauth.getIcd11CodeId() != null) {
                claim.getDiagnosisIds().add(preauth.getIcd11CodeId());
            }
        }
        if (claim.getDiagnosisIds().isEmpty()) {
            throw new IllegalArgumentException("diagnosisIds is required when no preauthorization is provided");
        }
        if (claim.getProcedureIds().isEmpty()) {
            throw new IllegalArgumentException("procedureIds are required");
        }
        claim.setInsurerId(insurerId);
        applyBaseConversion(claim);
        return toResponse(claimRepository.save(claim));
    }

    @Override
    public ClaimResponse attachInvoice(UUID id, AttachInvoiceRequest request) {
        Claim claim = getEntity(id);
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new IllegalStateException("Claim is not open for attaching invoices: " + claim.getStatus());
        }
        invoiceService.getEntityById(request.invoiceId());
        invoiceService.attachToClaim(request.invoiceId(), claim.getId());
        claim.getInvoiceIds().add(request.invoiceId());
        applyBaseConversion(claim);
        claim.setStatus(ClaimStatus.SUBMITTED);
        Claim saved = claimRepository.save(claim);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimResponse getById(UUID id) {
        Claim claim = getEntity(id);
        assertVisibleToCurrentUser(claim);
        return toResponse(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimResponse> list(Pageable pageable) {
        return findScoped(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimResponse> listByVisitor(UUID visitorId) {
        return claimRepository.findAllByVisitorId(visitorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimResponse> listByVisitor(UUID visitorId, Pageable pageable) {
        return claimRepository.findAllByVisitorId(visitorId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimExportRow> listForExport(ClaimStatus status, UUID insurerId,
                                              LocalDate dateFrom, LocalDate dateTo) {
        Instant from = dateFrom != null ? dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant to = dateTo != null ? dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Map<UUID, VisitorResponse> visitors = new HashMap<>();
        Map<UUID, String> providers = new HashMap<>();
        Map<UUID, String> benefits = new HashMap<>();
        Map<UUID, String> insurers = new HashMap<>();
        return claimRepository.findAllForExport(status, insurerId, from, to).stream()
                .map(claim -> {
                    VisitorResponse visitor = cached(visitors, claim.getVisitorId(), visitorService::getById);
                    return new ClaimExportRow(
                            visitor != null ? visitor.fullName() : null,
                            visitor != null ? visitor.passportNumber() : null,
                            cached(providers, claim.getServiceProviderId(),
                                    id -> serviceProviderService.getById(id).name()),
                            cached(benefits, claim.getBenefitId(), id -> benefitService.getById(id).benefitName()),
                            cached(insurers, claim.getInsurerId(), id -> insurerService.getById(id).name()),
                            claim.getClaimedAmount(),
                            claim.getCreatedDate());
                })
                .toList();
    }

    private <T> T cached(Map<UUID, T> cache, UUID id, Function<UUID, T> loader) {
        if (id == null) {
            return null;
        }
        return cache.computeIfAbsent(id, loader);
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
        return toResponse(saved);
    }

    @Override
    public ClaimResponse markPaid(UUID id) {
        Claim claim = getEntity(id);
        if (claim.getStatus() != ClaimStatus.APPROVED && claim.getStatus() != ClaimStatus.PARTIALLY_APPROVED) {
            throw new IllegalStateException("Only approved claims can be marked as paid");
        }
        claim.setStatus(ClaimStatus.PAID);
        return toResponse(claimRepository.save(claim));
    }

    @Override
    public void delete(UUID id) {
        claimRepository.delete(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimUtilizationTotal> sumClaimedAmountsByVisitorAndBenefit(
            Collection<UUID> visitorIds, Collection<UUID> benefitIds) {
        if (visitorIds.isEmpty() || benefitIds.isEmpty()) {
            return List.of();
        }
        return claimRepository.sumClaimedAmountsByVisitorAndBenefit(visitorIds, benefitIds);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimDashboardSummary dashboardSummary(ClaimStatus status, LocalDate dateFrom, LocalDate dateTo) {
        DashboardScope scope = resolveDashboardScope();
        if (scope.empty()) {
            return new ClaimDashboardSummary(0, BigDecimal.ZERO);
        }
        ClaimDashboardTotal total = claimRepository.summarize(status, scope.insurerId(), scope.serviceProviderId(),
                startOf(dateFrom), endOf(dateTo));
        return new ClaimDashboardSummary(total.getClaimCount(), total.getApprovedAmountUsd());
    }

    /**
     * Every insurer is listed, including those with no claims, plus an "Unknown" row for claims with no
     * insurer so the rows add up to the summary. An insurer user only ever sees their own row.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InsurerClaimDistribution> distributionByInsurer(
            ClaimStatus status, LocalDate dateFrom, LocalDate dateTo) {
        DashboardScope scope = resolveDashboardScope();
        if (scope.empty()) {
            return List.of();
        }
        Map<UUID, ClaimDashboardTotal> totals = new HashMap<>();
        ClaimDashboardTotal unknown = null;
        for (ClaimDashboardTotal row : claimRepository.summarizeByInsurer(status, scope.insurerId(),
                scope.serviceProviderId(), startOf(dateFrom), endOf(dateTo))) {
            if (row.getGroupId() == null) {
                unknown = row;
            } else {
                totals.put(row.getGroupId(), row);
            }
        }
        List<InsurerClaimDistribution> result = new ArrayList<>();
        for (InsurerResponse insurer : insurerService.listAll()) {
            if (scope.insurerId() != null && !scope.insurerId().equals(insurer.id())) {
                continue;
            }
            ClaimDashboardTotal total = totals.get(insurer.id());
            result.add(new InsurerClaimDistribution(insurer.id(), insurer.name(),
                    total != null ? total.getClaimCount() : 0,
                    total != null ? total.getApprovedAmountUsd() : BigDecimal.ZERO));
        }
        if (unknown != null) {
            result.add(new InsurerClaimDistribution(null, UNKNOWN_LABEL, unknown.getClaimCount(),
                    unknown.getApprovedAmountUsd()));
        }
        return result;
    }

    /** Same shape as {@link #distributionByInsurer}, grouped by service provider. */
    @Override
    @Transactional(readOnly = true)
    public List<ServiceProviderClaimDistribution> distributionByServiceProvider(
            ClaimStatus status, LocalDate dateFrom, LocalDate dateTo) {
        DashboardScope scope = resolveDashboardScope();
        if (scope.empty()) {
            return List.of();
        }
        Map<UUID, ClaimDashboardTotal> totals = new HashMap<>();
        ClaimDashboardTotal unknown = null;
        for (ClaimDashboardTotal row : claimRepository.summarizeByServiceProvider(status, scope.insurerId(),
                scope.serviceProviderId(), startOf(dateFrom), endOf(dateTo))) {
            if (row.getGroupId() == null) {
                unknown = row;
            } else {
                totals.put(row.getGroupId(), row);
            }
        }
        List<ServiceProviderClaimDistribution> result = new ArrayList<>();
        for (ServiceProviderResponse provider : serviceProviderService.listAll()) {
            if (scope.serviceProviderId() != null && !scope.serviceProviderId().equals(provider.id())) {
                continue;
            }
            ClaimDashboardTotal total = totals.get(provider.id());
            result.add(new ServiceProviderClaimDistribution(provider.id(), provider.name(),
                    total != null ? total.getClaimCount() : 0,
                    total != null ? total.getApprovedAmountUsd() : BigDecimal.ZERO));
        }
        if (unknown != null) {
            result.add(new ServiceProviderClaimDistribution(null, UNKNOWN_LABEL, unknown.getClaimCount(),
                    unknown.getApprovedAmountUsd()));
        }
        return result;
    }

    private void applyDecision(Claim claim, ClaimDecisionRequest request) {
        claim.setStatus(request.status());
        claim.setDecisionReason(request.reason());
        if (request.status() == ClaimStatus.REJECTED) {
            claim.setApprovedAmount(BigDecimal.ZERO);
            claim.setApprovedAmountBase(toApprovedBase(claim));
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
        claim.setApprovedAmountBase(toApprovedBase(claim));
    }

    /**
     * The approved amount scaled by the claim's base/claimed ratio, not by {@code exchangeRate}: with invoices
     * attached the claimed base is the sum of the invoices' base totals, which can differ from the claim's rate.
     */
    private static BigDecimal toApprovedBase(Claim claim) {
        BigDecimal approved = claim.getApprovedAmount();
        BigDecimal claimed = claim.getClaimedAmount();
        BigDecimal claimedBase = claim.getClaimedAmountBase();
        if (approved == null || claimedBase == null || claimed == null || claimed.signum() == 0) {
            return null;
        }
        return approved.multiply(claimedBase).divide(claimed, 2, RoundingMode.HALF_UP);
    }

    private UUID validateReferences(ClaimRequest request) {
        Policy policy = policyService.getEntityById(request.policyId());
        benefitService.getEntityById(request.benefitId());
        
        UUID insurerId = policy.getInsurerId();
        if (!insurerService.exists(insurerId)) {
            throw new ResourceNotFoundException("Insurer", insurerId);
        }

        if (request.visitorId() != null) {
            Visitor visitor = visitorService.getEntityById(request.visitorId());
            if (!policy.getId().equals(visitor.getPolicyId())) {
                throw new IllegalStateException("Visitor does not belong to the claim's policy");
            }
        }

        if (request.diagnosisIds() != null) {
            request.diagnosisIds().forEach(icd11CodeService::getEntityById);
        }
        if (request.procedureIds() != null) {
            request.procedureIds().forEach(procedureService::getById);
        }
        if (request.invoiceIds() != null) {
            request.invoiceIds().forEach(invoiceService::getEntityById);
        }
        return insurerId;
    }

    private void attachClaimToPreAuthorization(UUID claimId) {
        if (claimRepository.existsById(claimId)) {
            Claim  claim = getEntity(claimId);

            if (claim.getPreauthorizationId() != null) {
                preauthorizationService.markPreAuthorizationConvertedToClaim(claim.getPreauthorizationId());
            }

        }
    }

    private void applyBaseConversion(Claim claim) {
        BigDecimal rate = currencyConversionService.getExchangeRate(claim.getCurrency(), "USD");
        claim.setBaseCurrency("USD");
        claim.setExchangeRate(rate);
        claim.setFxRateDate(LocalDateTime.now());
        if (claim.getInvoiceIds().isEmpty()) {
            claim.setClaimedAmountBase(toBase(claim.getClaimedAmount(), rate));
        } else {
            claim.setClaimedAmountBase(aggregateInvoiceBaseAmounts(claim));
        }
        claim.setApprovedAmountBase(toApprovedBase(claim));
    }

    private BigDecimal aggregateInvoiceBaseAmounts(Claim claim) {
        BigDecimal total = BigDecimal.ZERO;
        for (UUID invoiceId : claim.getInvoiceIds()) {
            Invoice invoice = invoiceService.getEntityById(invoiceId);
            if (invoice.getBaseTotalAmount() != null) {
                total = total.add(invoice.getBaseTotalAmount());
            }
        }
        return total;
    }

    private static BigDecimal toBase(BigDecimal amount, BigDecimal rate) {
        if (amount == null) {
            return null;
        }
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
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

    private Page<Claim> findScoped(Pageable pageable) {
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user != null && "PROVIDER_USER".equals(user.role())) {
            UUID serviceProviderId = serviceProviderService.findIdByOrganizationId(user.organizationId())
                    .orElse(null);
            if (serviceProviderId == null) {
                return Page.empty(pageable);
            }
            return claimRepository.findAllByServiceProviderId(serviceProviderId, pageable);
        }
        if (user != null && "INSURER_USER".equals(user.role())) {
            UUID insurerId = insurerService.findIdByOrganizationId(user.organizationId()).orElse(null);
            if (insurerId == null) {
                return Page.empty(pageable);
            }
            return claimRepository.findAllByInsurerId(insurerId, pageable);
        }
        return claimRepository.findAll(pageable);
    }

    /**
     * Org-scoped users may only read their own organization's claims: an insurer user those raised
     * against their insurer, a provider user those raised by their service provider.
     */
    private void assertVisibleToCurrentUser(Claim claim) {
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user == null) {
            return;
        }
        if ("INSURER_USER".equals(user.role())) {
            UUID insurerId = insurerService.findIdByOrganizationId(user.organizationId()).orElse(null);
            if (insurerId == null || !insurerId.equals(claim.getInsurerId())) {
                throw new AccessDeniedException("Claim belongs to another insurer");
            }
        } else if ("PROVIDER_USER".equals(user.role())) {
            UUID serviceProviderId = serviceProviderService.findIdByOrganizationId(user.organizationId())
                    .orElse(null);
            if (serviceProviderId == null || !serviceProviderId.equals(claim.getServiceProviderId())) {
                throw new AccessDeniedException("Claim belongs to another service provider");
            }
        }
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

    @Override
    public Claim getEntityById(UUID id) {
        return getEntity(id);
    }

    private Claim getEntity(UUID id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", id));
    }

    private ClaimResponse toResponse(Claim claim) {
        VisitorResponse visitor = claim.getVisitorId() != null
                ? visitorService.getById(claim.getVisitorId())
                : null;
        InsurerResponse insurer = claim.getInsurerId() != null
                ? insurerService.getById(claim.getInsurerId())
                : null;
        List<InvoiceResponse> invoices = invoiceService.listByClaimId(claim.getId());
        List<Icd11CodeResponse> diagnoses = claim.getDiagnosisIds().stream()
                .map(this::resolveDiagnosis)
                .filter(Objects::nonNull)
                .toList();
        List<ProcedureResponse> procedures = claim.getProcedureIds().stream()
                .map(this::resolveProcedure)
                .filter(Objects::nonNull)
                .toList();
        return new ClaimResponse(
                claim.getId(),
                claim.getPolicyId(),
                claim.getBenefitId(),
                claim.getServiceProviderId(),
                claim.getPreauthorizationId(),
                claim.getVisitorId(),
                visitor,
                claim.getInsurerId(),
                insurer,
                claim.getClaimedAmount(),
                claim.getCurrency(),
                claim.getClaimedAmountBase(),
                claim.getExchangeRate(),
                claim.getBaseCurrency(),
                claim.getFxRateDate(),
                claim.getApprovedAmount(),
                claim.getDescription(),
                claim.getPrescription(),
                diagnoses,
                procedures,
                invoices,
                Set.copyOf(claim.getDocumentIds()),
                claim.getDecisionReason(),
                claim.getStatus(),
                claim.getCreatedDate(),
                claim.getUpdatedDate()
        );
    }

    private Icd11CodeResponse resolveDiagnosis(UUID id) {
        try {
            return icd11CodeService.getById(id);
        } catch (ResourceNotFoundException e) {
            log.warn("ICD-11 code {} not found for claim, skipping", id);
            return null;
        }
    }

    private ProcedureResponse resolveProcedure(UUID id) {
        try {
            return procedureService.getById(id);
        } catch (ResourceNotFoundException e) {
            log.warn("Procedure {} not found for claim, skipping", id);
            return null;
        }
    }
}
