package com.travel.insurance.claim;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.dto.AttachInvoiceRequest;
import com.travel.insurance.claim.dto.ClaimDashboardSummary;
import com.travel.insurance.claim.dto.ClaimDecisionRequest;
import com.travel.insurance.claim.dto.ClaimExportRow;
import com.travel.insurance.claim.dto.ClaimListSummary;
import com.travel.insurance.claim.dto.ClaimSearchCriteria;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.claim.dto.ConvertInvoiceRequest;
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
import com.travel.insurance.invoice.InvoiceChangedEvent;
import com.travel.insurance.claim.dto.ClaimUpdateRequest;
import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import com.travel.insurance.invoice.dto.InvoiceItemResponse;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.medicalservice.MedicalServiceService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final MedicalServiceService medicalServiceService;
    private final Icd11CodeService icd11CodeService;
    private final ProcedureService procedureService;
    private final CurrencyConversionService currencyConversionService;
    private final EventPublisher eventPublisher;
    private final PreauthorizationService preauthorizationService;
    private final ApplicationEventPublisher applicationEventPublisher;

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
     * Claim fields are copied from the pre-authorization; the invoice lines come from the request
     * and their total is the claimed amount, capped at the approved amount.
     * Unlike {@link #create}, procedureIds are not required — the pre-auth never captured any.
     */
    @Override
    public ClaimResponse createFromPreauthorization(UUID preauthorizationId, ConvertPreauthorizationRequest request) {
        PreauthorizationResponse preauth = preauthorizationService.getById(preauthorizationId);
        assertProviderOwns(preauth);
        if (!CONVERTIBLE_PREAUTH_STATUSES.contains(preauth.status())) {
            throw new IllegalStateException(
                    "Only approved pre-authorizations can be converted to a claim: " + preauth.status());
        }
        if (Boolean.TRUE.equals(preauth.convertedToClaim())) {
            throw new IllegalStateException("Pre-authorization has already been converted to a claim");
        }

        Set<String> seenNumbers = new HashSet<>();
        for (ConvertInvoiceRequest invoice : request.invoices()) {
            String number = invoice.invoiceNumber().trim();
            if (!seenNumbers.add(number.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate invoice number in request: " + number);
            }
        }

        Map<ConvertInvoiceRequest, List<InvoiceItemRequest>> itemsByInvoice = new LinkedHashMap<>();
        for (ConvertInvoiceRequest invoice : request.invoices()) {
            itemsByInvoice.put(invoice, invoice.invoiceItems().stream()
                    .map(item -> item.medicalServiceId() != null ? item
                            : new InvoiceItemRequest(preauth.medicalServiceId(), item.description(),
                                    item.quantity(), item.unitPrice(), item.amount(), item.serviceDate()))
                    .toList());
        }
        BigDecimal invoiceTotal = itemsByInvoice.values().stream().flatMap(List::stream)
                .map(InvoiceItemRequest::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal authorizedAmount = preauth.approvedAmount() != null ? preauth.approvedAmount() : preauth.requestedAmount();
        if (invoiceTotal.compareTo(authorizedAmount) > 0) {
            throw new IllegalArgumentException("Invoice total " + invoiceTotal.toPlainString()
                    + " exceeds the pre-authorized amount " + authorizedAmount.toPlainString());
        }

        Claim claim = new Claim();
        claim.setPolicyId(preauth.policyId());
        claim.setInsurerId(preauth.insurerId());
        claim.setVisitorId(preauth.visitorId());
        claim.setBenefitId(preauth.benefitId());
        claim.setServiceProviderId(preauth.serviceProviderId());
        claim.setPreauthorizationId(preauth.id());
        claim.setClaimedAmount(invoiceTotal);
        claim.setDescription(preauth.serviceDescription());
        claim.setVisitType(request.visitType() != null ? request.visitType() : preauth.visitType());
        if (preauth.icd11CodeId() != null) {
            claim.getDiagnosisIds().add(preauth.icd11CodeId());
        }
        claim.setStatus(ClaimStatus.OPEN);
        applyBaseConversion(claim);
        claim = claimRepository.save(claim);

        for (Map.Entry<ConvertInvoiceRequest, List<InvoiceItemRequest>> entry : itemsByInvoice.entrySet()) {
            ConvertInvoiceRequest invoice = entry.getKey();
            BigDecimal total = entry.getValue().stream()
                    .map(InvoiceItemRequest::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            invoiceService.create(new InvoiceRequest(claim.getId(), invoice.invoiceNumber().trim(),
                    invoice.issueDate(), claim.getCurrency(), total, entry.getValue()));
        }

        preauthorizationService.markPreAuthorizationConvertedToClaim(preauth.id());
        applicationEventPublisher.publishEvent(new PreauthorizationConvertedToClaimEvent(claim.getId(), preauth.id()));
        return toResponse(claim);
    }

    /** REQUIRES_NEW: called from AFTER_COMMIT listeners, where joining the finished transaction would lose the write. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSentToMinet(UUID id) {
        Claim claim = getEntity(id);
        claim.setSentToMinet(true);
        claimRepository.save(claim);
    }

    @Override
    public ClaimResponse update(UUID id, ClaimUpdateRequest request) {
        Claim claim = getEntity(id);
        assertVisibleToCurrentUser(claim);
        assertEditable(claim);
        if (request.diagnosisIds() == null || request.diagnosisIds().isEmpty()) {
            throw new IllegalArgumentException("diagnosisIds is required");
        }
        request.diagnosisIds().forEach(icd11CodeService::getEntityById);
        if (request.procedureIds() != null) {
            request.procedureIds().forEach(procedureService::getById);
        }
        claim.setDescription(request.description());
        claim.setPrescription(request.prescription());
        claim.setDiagnosisIds(new HashSet<>(request.diagnosisIds()));
        claim.setProcedureIds(request.procedureIds() == null ? new HashSet<>() : new HashSet<>(request.procedureIds()));
        if (request.visitType() != null) {
            claim.setVisitType(request.visitType());
        }
        if (request.serviceProviderId() != null
                && !request.serviceProviderId().equals(claim.getServiceProviderId())) {
            String role = SecurityUtils.currentUser().map(AuthenticatedUser::role).orElse(null);
            if (!"ADMIN".equals(role) && !"CONTACT_CENTER".equals(role)) {
                throw new AccessDeniedException("Only an admin or contact centre user can change a claim's provider");
            }
            if (!serviceProviderService.exists(request.serviceProviderId())) {
                throw new ResourceNotFoundException("ServiceProvider", request.serviceProviderId());
            }
            claim.setServiceProviderId(request.serviceProviderId());
            // Keep the source pre-auth with the claim so the new provider can still see it.
            if (claim.getPreauthorizationId() != null) {
                preauthorizationService.reassignServiceProvider(
                        claim.getPreauthorizationId(), request.serviceProviderId());
            }
        }
        return toResponse(claimRepository.save(claim));
    }

    /**
     * Keeps the claimed amount equal to the sum of the claim's invoices. Runs inside the invoice
     * transaction, so throwing here (claim locked) rolls the invoice change back with a 409.
     */
    @EventListener
    public void onInvoiceChanged(InvoiceChangedEvent event) {
        Claim claim = getEntity(event.claimId());
        assertEditable(claim);
        recalculateClaimedAmount(claim);
        claimRepository.save(claim);
    }

    private static void assertEditable(Claim claim) {
        if (!claim.getStatus().isEditable()) {
            throw new IllegalStateException("Claim can no longer be edited in status " + claim.getStatus());
        }
    }

    /**
     * Claimed amount := sum of the claim's invoices (in the claim currency), base amount := sum of the
     * invoices' base totals. A claim with no invoices keeps the amount it was raised with.
     */
    private void recalculateClaimedAmount(Claim claim) {
        List<InvoiceResponse> invoices = invoiceService.listByClaimId(claim.getId());
        if (invoices.isEmpty()) {
            return;
        }
        applyBaseConversion(claim);
        BigDecimal rate = claim.getExchangeRate();
        BigDecimal claimed = BigDecimal.ZERO;
        BigDecimal base = BigDecimal.ZERO;
        for (InvoiceResponse invoice : invoices) {
            BigDecimal invoiceBase = invoice.baseTotalAmount() != null ? invoice.baseTotalAmount() : BigDecimal.ZERO;
            base = base.add(invoiceBase);
            if (invoice.totalAmount() != null && Objects.equals(invoice.currency(), claim.getCurrency())) {
                claimed = claimed.add(invoice.totalAmount());
            } else if (rate != null && rate.signum() > 0) {
                claimed = claimed.add(invoiceBase.divide(rate, 2, RoundingMode.HALF_UP));
            }
        }
        claim.setClaimedAmount(claimed.setScale(2, RoundingMode.HALF_UP));
        claim.setClaimedAmountBase(base.setScale(2, RoundingMode.HALF_UP));
        claim.setApprovedAmountBase(toApprovedBase(claim));
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
    public Page<ClaimResponse> search(ClaimSearchCriteria criteria, Pageable pageable) {
        DashboardScope scope = resolveDashboardScope();
        if (scope.empty()) {
            return Page.empty(pageable);
        }
        return claimRepository.findAll(buildSpecification(criteria, scope), pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimListSummary summary(ClaimSearchCriteria criteria) {
        Map<ClaimStatus, Long> counts = new EnumMap<>(ClaimStatus.class);
        for (ClaimStatus status : ClaimStatus.values()) {
            counts.put(status, 0L);
        }
        DashboardScope scope = resolveDashboardScope();
        if (scope.empty()) {
            return new ClaimListSummary(0, counts, 0, BigDecimal.ZERO, 0, 0);
        }
        Specification<Claim> filtered = buildSpecification(criteria.withoutStatus(), scope);
        counts.putAll(claimRepository.countByStatus(filtered));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return new ClaimListSummary(0, counts, 0, BigDecimal.ZERO, 0, 0);
        }
        long awaiting = OPEN_STATUSES.stream().mapToLong(counts::get).sum();
        BigDecimal totalValue = claimRepository.sumClaimedAmountBase(filtered);
        long needsAttention = awaiting == 0 ? 0 : claimRepository.count(filtered
                .and(ClaimSpecifications.statusIn(OPEN_STATUSES))
                .and(ClaimSpecifications.needsAttention(EnumSet.allOf(ClaimAttentionFlag.class))));
        long notSentToMinet = claimRepository.count(filtered.and(ClaimSpecifications.sentToMinet(false)));
        return new ClaimListSummary(total, counts, awaiting, totalValue, needsAttention, notSentToMinet);
    }

    private Specification<Claim> buildSpecification(ClaimSearchCriteria criteria, DashboardScope scope) {
        List<Specification<Claim>> specs = new ArrayList<>();
        if (scope.insurerId() != null) {
            specs.add(ClaimSpecifications.insurer(scope.insurerId()));
        }
        if (scope.serviceProviderId() != null) {
            specs.add(ClaimSpecifications.serviceProvider(scope.serviceProviderId()));
        }
        if (criteria.status() != null) {
            specs.add(ClaimSpecifications.statusIn(EnumSet.of(criteria.status())));
        }
        if (criteria.dateFrom() != null || criteria.dateTo() != null) {
            specs.add(ClaimSpecifications.createdBetween(startOf(criteria.dateFrom()), endOf(criteria.dateTo())));
        }
        if (hasAny(criteria.benefitIds())) {
            specs.add(ClaimSpecifications.attributeIn("benefitId", criteria.benefitIds()));
        }
        boolean providerUnlinked = Boolean.TRUE.equals(criteria.providerUnlinked());
        if (hasAny(criteria.serviceProviderIds()) || providerUnlinked) {
            specs.add(ClaimSpecifications.providers(criteria.serviceProviderIds(), providerUnlinked));
        }
        if (hasAny(criteria.insurerIds())) {
            specs.add(ClaimSpecifications.attributeIn("insurerId", criteria.insurerIds()));
        }
        if (criteria.minAmountBase() != null || criteria.maxAmountBase() != null) {
            specs.add(ClaimSpecifications.amountBaseBetween(criteria.minAmountBase(), criteria.maxAmountBase()));
        }
        if (criteria.sentToMinet() != null) {
            specs.add(ClaimSpecifications.sentToMinet(criteria.sentToMinet()));
        }
        if (hasAny(criteria.flags())) {
            specs.add(ClaimSpecifications.needsAttention(criteria.flags()));
        }
        if (criteria.hasSearch()) {
            specs.add(searchSpecification(criteria.q().trim()));
        }
        return Specification.allOf(specs);
    }

    /** Resolves free text into ids through the owning features' services; see {@link ClaimSearchCriteria}. */
    private Specification<Claim> searchSpecification(String q) {
        List<UUID> claimIds = new ArrayList<>(invoiceService.findClaimIdsByInvoiceNumber(q));
        parseUuid(q).ifPresent(claimIds::add);
        List<UUID> visitorIds = visitorService.findIdByPassportNumber(q).map(List::of).orElse(List.of());
        List<UUID> diagnosisIds = icd11CodeService.search(q, PageRequest.of(0, 200)).getContent().stream()
                .map(Icd11CodeResponse::id)
                .toList();
        return ClaimSpecifications.search(claimIds, visitorIds, diagnosisIds);
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static boolean hasAny(Collection<?> values) {
        return values != null && !values.isEmpty();
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
        Map<UUID, Icd11CodeResponse> diagnoses = new HashMap<>();
        Map<UUID, String> departments = new HashMap<>();
        return claimRepository.findAllForExport(status, insurerId, from, to).stream()
                .map(claim -> {
                    VisitorResponse visitor = cached(visitors, claim.getVisitorId(), visitorService::getById);
                    String providerName = cached(providers, claim.getServiceProviderId(),
                            id -> serviceProviderService.getById(id).name());
                    String benefitName = cached(benefits, claim.getBenefitId(),
                            id -> benefitService.getById(id).benefitName());
                    List<Icd11CodeResponse> claimDiagnoses = claim.getDiagnosisIds().stream()
                            .map(id -> diagnoses.computeIfAbsent(id, this::resolveDiagnosis))
                            .filter(Objects::nonNull)
                            .toList();
                    String codes = claimDiagnoses.stream().map(Icd11CodeResponse::code)
                            .collect(Collectors.joining("; "));
                    String descriptions = claimDiagnoses.stream().map(Icd11CodeResponse::title)
                            .collect(Collectors.joining("; "));
                    List<InvoiceResponse> invoices = invoiceService.listByClaimId(claim.getId());
                    List<InvoiceItemResponse> items = invoices.stream()
                            .flatMap(invoice -> invoice.invoiceItems().stream())
                            .toList();
                    BigDecimal invoiceAmount = invoices.stream()
                            .map(InvoiceResponse::totalAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal::add)
                            .orElse(null);
                    return new ClaimExportRow(
                            providerName,
                            claim.getServiceProviderId(),
                            benefitName,
                            visitor != null ? visitor.passportNumber() : null,
                            claim.getCreatedDate(),
                            claim.getVisitType(),
                            codes,
                            descriptions,
                            items.stream().map(InvoiceItemResponse::serviceDate).filter(Objects::nonNull)
                                    .min(Comparator.naturalOrder()).orElse(null),
                            claim.getClaimedAmount(),
                            joinDistinct(invoices.stream().map(InvoiceResponse::invoiceNumber)),
                            invoiceAmount,
                            joinDistinct(items.stream().map(InvoiceItemResponse::medicalServiceName)),
                            joinDistinct(items.stream().map(item -> department(departments, item.medicalServiceId()))),
                            claim.getClaimedAmountBase(),
                            claim.getExchangeRate(),
                            claim.getStatus());
                })
                .toList();
    }

    /** Distinct non-blank values in encounter order joined with {@code "; "}, or null when there are none. */
    private static String joinDistinct(java.util.stream.Stream<String> values) {
        String joined = values.filter(v -> v != null && !v.isBlank()).distinct()
                .collect(Collectors.joining("; "));
        return joined.isEmpty() ? null : joined;
    }

    private String department(Map<UUID, String> cache, UUID medicalServiceId) {
        if (medicalServiceId == null) {
            return null;
        }
        return cache.computeIfAbsent(medicalServiceId, id -> {
            try {
                return medicalServiceService.getById(id).departmentName();
            } catch (ResourceNotFoundException e) {
                log.warn("Medical service {} not found for claims export, leaving department blank", id);
                return null;
            }
        });
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
    @Transactional
    public boolean applyMinetStatusUpdate(
            UUID id, String eventId, String status, Instant changedAt, List<String> comments) {
        Claim claim = getEntity(id);
        if (eventId != null && eventId.equals(claim.getMinetLastEventId())) {
            log.info("Minet event {} already applied to claim {}; ignoring", eventId, id);
            return false;
        }
        if (changedAt != null && claim.getStatusChangedAt() != null && changedAt.isBefore(claim.getStatusChangedAt())) {
            log.info("Minet event {} for claim {} is older than the last update; ignoring", eventId, id);
            return false;
        }
        if (status != null && !status.isBlank()) {
            Optional<ClaimStatus> mapped = ClaimStatus.fromMinet(status);
            if (mapped.isPresent()) {
                claim.setStatus(mapped.get());
            } else {
                log.warn("Minet sent unknown status '{}' for claim {}; status left unchanged", status, id);
            }
        }
        if (changedAt != null) {
            claim.setStatusChangedAt(changedAt);
        }
        if (comments != null) {
            String joined = comments.stream().filter(c -> c != null && !c.isBlank())
                    .collect(Collectors.joining("\n"));
            if (!joined.isEmpty()) {
                claim.setStatusComments(joined);
            }
        }
        if (eventId != null) {
            claim.setMinetLastEventId(eventId);
        }
        claimRepository.save(claim);
        return true;
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

    /**
     * Same shape as {@link #distributionByInsurer}, grouped by service provider. With a {@code limit} only
     * the top providers by claim count (ties broken by approved USD) are returned: providers with no claims
     * and the "Unknown" row are dropped.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ServiceProviderClaimDistribution> distributionByServiceProvider(
            ClaimStatus status, LocalDate dateFrom, LocalDate dateTo, Integer limit) {
        if (limit != null && limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
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
        if (limit != null) {
            return result.stream()
                    .filter(row -> row.claimCount() > 0)
                    .sorted(Comparator.comparingLong(ServiceProviderClaimDistribution::claimCount).reversed()
                            .thenComparing(ServiceProviderClaimDistribution::approvedAmountUsd,
                                    Comparator.reverseOrder()))
                    .limit(limit)
                    .toList();
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

    /** A provider may only act on its own facility's pre-auths; 404 rather than 403 so ids don't leak. */
    private void assertProviderOwns(PreauthorizationResponse preauth) {
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user == null || !"PROVIDER_USER".equals(user.role())) {
            return;
        }
        UUID serviceProviderId = serviceProviderService.findIdByOrganizationId(user.organizationId()).orElse(null);
        if (!Objects.equals(serviceProviderId, preauth.serviceProviderId())) {
            throw new ResourceNotFoundException("Preauthorization not found: " + preauth.id());
        }
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
                claim.getUpdatedDate(),
                claim.getVisitType(),
                claim.isSentToMinet(),
                claim.getStatusChangedAt(),
                claim.getStatusComments()
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
