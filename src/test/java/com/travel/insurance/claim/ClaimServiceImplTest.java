package com.travel.insurance.claim;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.dto.BenefitResponse;
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
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.insurer.dto.InsurerResponse;
import com.travel.insurance.invoice.Invoice;
import com.travel.insurance.invoice.InvoiceService;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.preauthorization.Preauthorization;
import com.travel.insurance.preauthorization.PreauthorizationService;
import com.travel.insurance.preauthorization.PreauthorizationStatus;
import com.travel.insurance.preauthorization.dto.PreauthorizationItemResponse;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import com.travel.insurance.procedure.ProcedureService;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.dto.VisitorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private BenefitService benefitService;

    @Mock
    private VisitorService visitorService;

    @Mock
    private InsurerService insurerService;

    @Mock
    private ServiceProviderService serviceProviderService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private Icd11CodeService icd11CodeService;

    @Mock
    private ProcedureService procedureService;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private PreauthorizationService preauthorizationService;

    private final ClaimMapper claimMapper = new ClaimMapper();

    private ClaimServiceImpl claimService;

    private UUID policyId;
    private UUID benefitId;
    private UUID visitorId;
    private UUID insurerId;
    private UUID invoiceId;
    private UUID documentId;
    private UUID diagnosisId1;
    private UUID diagnosisId2;
    private UUID procedureId1;

    private static final BigDecimal KES_TO_USD = new BigDecimal("0.0077");

    @BeforeEach
    void setUp() {
        claimService = new ClaimServiceImpl(
                claimRepository, claimMapper, policyService, benefitService,
                visitorService, insurerService, serviceProviderService, invoiceService,
                icd11CodeService, procedureService,
                currencyConversionService, eventPublisher, preauthorizationService);
        policyId = UUID.randomUUID();
        benefitId = UUID.randomUUID();
        visitorId = UUID.randomUUID();
        insurerId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        diagnosisId1 = UUID.randomUUID();
        diagnosisId2 = UUID.randomUUID();
        procedureId1 = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID organizationId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), organizationId, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private Policy policyCoveredBy(UUID insurerId) {
        Policy policy = new Policy();
        policy.setId(policyId);
        policy.setInsurerId(insurerId);
        return policy;
    }

    private ClaimRequest baseRequest() {
        return new ClaimRequest(
                policyId, benefitId, null, null, null,
                new BigDecimal("50000.00"), null, null,
                Set.of(diagnosisId1), Set.of(procedureId1), null, null);
    }

    private ClaimRequest fullRequest() {
        return new ClaimRequest(
                policyId, benefitId, null, null, visitorId,
                new BigDecimal("50000.00"), "Hospital stay",
                "Paracetamol 500mg twice daily",
                Set.of(diagnosisId1, diagnosisId2),
                Set.of(procedureId1),
                Set.of(invoiceId),
                Set.of(documentId));
    }

    private Visitor visitorOnPolicy() {
        Visitor visitor = new Visitor();
        visitor.setId(visitorId);
        visitor.setPolicyId(policyId);
        return visitor;
    }

    private VisitorResponse visitorResponse() {
        return new VisitorResponse(visitorId, policyId, insurerId, "Jane Traveler", "P1234567",
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, Instant.now(), Instant.now());
    }

    private InsurerResponse insurerResponse() {
        return new InsurerResponse(insurerId, policyId, "Jubilee Insurance", null, null, null, null,
                null, null, null, null, null, null, null, Instant.now(), Instant.now());
    }

    private InvoiceResponse invoiceResponse() {
        return new InvoiceResponse(invoiceId, null, "INV-2026-001", null, "KES",
                new BigDecimal("45000.00"), KES_TO_USD, "USD", new BigDecimal("346.50"),
                LocalDateTime.now(), null, Instant.now(), Instant.now());
    }

    private Invoice invoiceWithBaseTotal(String baseTotal) {
        Invoice invoice = new Invoice();
        invoice.setBaseTotalAmount(new BigDecimal(baseTotal));
        return invoice;
    }

    private Icd11CodeResponse icd11Response(UUID id) {
        return new Icd11CodeResponse(id, "B54", "Malaria", Instant.now(), Instant.now());
    }

    private ProcedureResponse procedureResponse(UUID id) {
        return new ProcedureResponse(id, "P-001", "Blood smear",
                "Malaria microscopy", UUID.randomUUID(), true, null, Instant.now(), Instant.now());
    }

    @Test
    void createSavesClaimWithAllReferenceFields() {
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(visitorService.getEntityById(visitorId)).thenReturn(visitorOnPolicy());
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(invoiceService.getEntityById(invoiceId)).thenReturn(invoiceWithBaseTotal("45000.00"));
        when(icd11CodeService.getEntityById(diagnosisId1)).thenReturn(null);
        when(icd11CodeService.getEntityById(diagnosisId2)).thenReturn(null);
        when(icd11CodeService.getById(diagnosisId1)).thenReturn(icd11Response(diagnosisId1));
        when(icd11CodeService.getById(diagnosisId2)).thenReturn(icd11Response(diagnosisId2));
        when(procedureService.getById(procedureId1)).thenReturn(procedureResponse(procedureId1));
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceService.listByClaimId(any())).thenReturn(List.of(invoiceResponse()));
        when(visitorService.getById(visitorId)).thenReturn(visitorResponse());
        when(insurerService.getById(insurerId)).thenReturn(insurerResponse());

        ClaimResponse response = claimService.create(fullRequest());

        assertThat(response.visitorId()).isEqualTo(visitorId);
        assertThat(response.insurerId()).isEqualTo(insurerId);
        assertThat(response.prescription()).isEqualTo("Paracetamol 500mg twice daily");
        assertThat(response.diagnoses()).extracting(Icd11CodeResponse::code).containsExactly("B54", "B54");
        assertThat(response.procedures()).extracting(ProcedureResponse::procedureCode).containsExactly("P-001");
        assertThat(response.documentIds()).containsExactly(documentId);
        assertThat(response.status()).isEqualTo(ClaimStatus.OPEN);
        assertThat(response.visitor().fullName()).isEqualTo("Jane Traveler");
        assertThat(response.insurer().name()).isEqualTo("Jubilee Insurance");
        assertThat(response.invoices()).extracting(InvoiceResponse::invoiceNumber)
                .containsExactly("INV-2026-001");
        assertThat(response.claimedAmount()).isEqualByComparingTo("50000.00");
        assertThat(response.currency()).isEqualTo("KES");
        assertThat(response.baseCurrency()).isEqualTo("USD");
        assertThat(response.claimedAmountBase()).isEqualByComparingTo("45000.00");
        assertThat(response.exchangeRate()).isEqualByComparingTo("0.0077");
        assertThat(response.fxRateDate()).isNotNull();
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    void createConvertsRawClaimedAmountWhenNoInvoicesAttached() {
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(icd11CodeService.getEntityById(diagnosisId1)).thenReturn(null);
        when(procedureService.getById(procedureId1)).thenReturn(procedureResponse(procedureId1));
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(insurerService.getById(insurerId)).thenReturn(insurerResponse());
        when(invoiceService.listByClaimId(any())).thenReturn(List.of());

        ClaimResponse response = claimService.create(baseRequest());

        assertThat(response.claimedAmount()).isEqualByComparingTo("50000.00");
        assertThat(response.currency()).isEqualTo("KES");
        assertThat(response.baseCurrency()).isEqualTo("USD");
        assertThat(response.claimedAmountBase()).isEqualByComparingTo("385.00");
        assertThat(response.exchangeRate()).isEqualByComparingTo("0.0077");
        assertThat(response.fxRateDate()).isNotNull();
    }

    @Test
    void createRejectsEmptyDiagnosisIds() {
        ClaimRequest request = new ClaimRequest(
                policyId, benefitId, null, null, null,
                new BigDecimal("50000.00"), null, null,
                null, Set.of(procedureId1), null, null);

        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(procedureService.getById(procedureId1)).thenReturn(procedureResponse(procedureId1));

        assertThatThrownBy(() -> claimService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnosisIds");
        verify(claimRepository, never()).save(any());
    }

    @Test
    void createRejectsEmptyProcedureIds() {
        ClaimRequest request = new ClaimRequest(
                policyId, benefitId, null, null, null,
                new BigDecimal("50000.00"), null, null,
                Set.of(diagnosisId1), null, null, null);

        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(icd11CodeService.getEntityById(diagnosisId1)).thenReturn(null);

        assertThatThrownBy(() -> claimService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("procedureIds");
        verify(claimRepository, never()).save(any());
    }

    @Test
    void createAutoPopulatesDiagnosisFromPreauthorization() {
        UUID preauthId = UUID.randomUUID();
        UUID icd11Id = UUID.randomUUID();
        ClaimRequest request = new ClaimRequest(
                policyId, benefitId, null, preauthId, null,
                new BigDecimal("50000.00"), null, null,
                null, Set.of(procedureId1), null, null);

        Preauthorization preauth = new Preauthorization();
        preauth.setId(preauthId);
        preauth.setIcd11CodeId(icd11Id);

        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(procedureService.getById(procedureId1)).thenReturn(procedureResponse(procedureId1));
        when(preauthorizationService.getEntityById(preauthId)).thenReturn(preauth);
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(insurerService.getById(insurerId)).thenReturn(insurerResponse());
        when(icd11CodeService.getById(icd11Id)).thenReturn(icd11Response(icd11Id));
        when(invoiceService.listByClaimId(any())).thenReturn(List.of());

        ClaimResponse response = claimService.create(request);

        assertThat(response.diagnoses()).hasSize(1);
        assertThat(response.diagnoses().getFirst().id()).isEqualTo(icd11Id);
        verify(preauthorizationService).getEntityById(preauthId);
    }

    @Test
    void createRejectsVisitorNotOnClaimPolicy() {
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(insurerService.exists(insurerId)).thenReturn(true);
        Visitor visitorOnOtherPolicy = new Visitor();
        visitorOnOtherPolicy.setId(visitorId);
        visitorOnOtherPolicy.setPolicyId(UUID.randomUUID());
        when(visitorService.getEntityById(visitorId)).thenReturn(visitorOnOtherPolicy);

        assertThatThrownBy(() -> claimService.create(fullRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Visitor does not belong to the claim's policy");
        verify(claimRepository, never()).save(any());
    }

    @Test
    void createRejectsDerivedInsurerThatDoesNotExist() {
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(insurerService.exists(insurerId)).thenReturn(false);

        assertThatThrownBy(() -> claimService.create(baseRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(claimRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownInvoice() {
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(visitorService.getEntityById(visitorId)).thenReturn(visitorOnPolicy());
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(invoiceService.getEntityById(invoiceId))
                .thenThrow(new ResourceNotFoundException("Invoice", invoiceId));
        when(icd11CodeService.getEntityById(diagnosisId1)).thenReturn(null);
        when(icd11CodeService.getEntityById(diagnosisId2)).thenReturn(null);
        when(procedureService.getById(procedureId1)).thenReturn(procedureResponse(procedureId1));

        assertThatThrownBy(() -> claimService.create(fullRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(claimRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownDiagnosis() {
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(icd11CodeService.getEntityById(diagnosisId1))
                .thenThrow(new ResourceNotFoundException("Icd11Code", diagnosisId1));

        assertThatThrownBy(() -> claimService.create(baseRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(claimRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownProcedure() {
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(icd11CodeService.getEntityById(diagnosisId1)).thenReturn(null);
        when(procedureService.getById(procedureId1))
                .thenThrow(new ResourceNotFoundException("Procedure", procedureId1));

        assertThatThrownBy(() -> claimService.create(baseRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(claimRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsClaim() {
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(invoiceService.listByClaimId(id)).thenReturn(List.of());

        ClaimResponse response = claimService.getById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(ClaimStatus.SUBMITTED);
    }

    @Test
    void listForExportResolvesNamesAndLooksEachReferenceUpOnce() {
        UUID providerId = UUID.randomUUID();
        Claim first = claimMapper.toEntity(baseRequest());
        first.setVisitorId(visitorId);
        first.setBenefitId(benefitId);
        first.setServiceProviderId(providerId);
        first.setInsurerId(insurerId);
        first.setClaimedAmount(new BigDecimal("1000.00"));
        Claim second = claimMapper.toEntity(baseRequest());
        second.setVisitorId(visitorId);
        second.setBenefitId(benefitId);
        second.setServiceProviderId(providerId);
        second.setInsurerId(insurerId);
        second.setClaimedAmount(new BigDecimal("250.50"));
        Instant from = LocalDate.of(2026, 10, 1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant to = LocalDate.of(2026, 11, 1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        when(claimRepository.findAllForExport(ClaimStatus.APPROVED, insurerId, from, to))
                .thenReturn(List.of(first, second));
        when(visitorService.getById(visitorId)).thenReturn(visitorResponse());
        when(serviceProviderService.getById(providerId)).thenReturn(new ServiceProviderResponse(providerId,
                "Aga Khan Hospital", null, null, null, null, null, null, null, Instant.now(), Instant.now()));
        when(benefitService.getById(benefitId)).thenReturn(
                new BenefitResponse(benefitId, "Outpatient", new BigDecimal("5000"), Instant.now(), Instant.now()));
        when(insurerService.getById(insurerId)).thenReturn(insurerResponse());

        List<ClaimExportRow> rows = claimService.listForExport(
                ClaimStatus.APPROVED, insurerId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).visitorName()).isEqualTo("Jane Traveler");
        assertThat(rows.get(0).passportNumber()).isEqualTo("P1234567");
        assertThat(rows.get(0).serviceProviderName()).isEqualTo("Aga Khan Hospital");
        assertThat(rows.get(0).benefitName()).isEqualTo("Outpatient");
        assertThat(rows.get(0).insurerName()).isEqualTo("Jubilee Insurance");
        assertThat(rows.get(0).amount()).isEqualByComparingTo("1000.00");
        assertThat(rows.get(1).amount()).isEqualByComparingTo("250.50");
        verify(visitorService, org.mockito.Mockito.times(1)).getById(visitorId);
        verify(serviceProviderService, org.mockito.Mockito.times(1)).getById(providerId);
    }

    @Test
    void listForExportLeavesNamesBlankWhenClaimHasNoVisitorOrProvider() {
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setVisitorId(null);
        claim.setBenefitId(benefitId);
        claim.setServiceProviderId(null);
        claim.setInsurerId(null);
        when(claimRepository.findAllForExport(null, null, null, null)).thenReturn(List.of(claim));
        when(benefitService.getById(benefitId)).thenReturn(
                new BenefitResponse(benefitId, "Outpatient", new BigDecimal("5000"), Instant.now(), Instant.now()));

        List<ClaimExportRow> rows = claimService.listForExport(null, null, null, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).visitorName()).isNull();
        assertThat(rows.get(0).serviceProviderName()).isNull();
        assertThat(rows.get(0).insurerName()).isNull();
        assertThat(rows.get(0).benefitName()).isEqualTo("Outpatient");
    }

    @Test
    void listByVisitorReturnsClaimsForThatVisitor() {
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(UUID.randomUUID());
        claim.setVisitorId(visitorId);
        when(claimRepository.findAllByVisitorId(visitorId)).thenReturn(List.of(claim));
        when(visitorService.getById(visitorId)).thenReturn(visitorResponse());
        when(invoiceService.listByClaimId(any())).thenReturn(List.of());

        List<ClaimResponse> responses = claimService.listByVisitor(visitorId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().visitorId()).isEqualTo(visitorId);
    }

    @Test
    void listScopesToServiceProviderForProviderUserViaOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        UUID serviceProviderId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(serviceProviderId));
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 10);
        when(claimRepository.findAllByServiceProviderId(serviceProviderId, pageable))
                .thenReturn(new PageImpl<>(List.of(claim)));
        when(invoiceService.listByClaimId(any())).thenReturn(List.of());

        Page<ClaimResponse> result = claimService.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(claimRepository).findAllByServiceProviderId(serviceProviderId, pageable);
        verify(claimRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listScopesToInsurerForInsurerUserViaOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(insurerId));
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(UUID.randomUUID());
        claim.setInsurerId(insurerId);
        Pageable pageable = PageRequest.of(0, 10);
        when(claimRepository.findAllByInsurerId(insurerId, pageable)).thenReturn(new PageImpl<>(List.of(claim)));
        when(insurerService.getById(insurerId)).thenReturn(insurerResponse());
        when(invoiceService.listByClaimId(any())).thenReturn(List.of());

        Page<ClaimResponse> result = claimService.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(claimRepository).findAllByInsurerId(insurerId, pageable);
        verify(claimRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listReturnsEmptyForInsurerUserWithoutInsurer() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 10);

        Page<ClaimResponse> result = claimService.list(pageable);

        assertThat(result.getContent()).isEmpty();
        verify(claimRepository, never()).findAll(any(Pageable.class));
        verify(claimRepository, never()).findAllByInsurerId(any(), any());
    }

    @Test
    void listReturnsAllClaimsForAdmin() {
        authenticateAs(null, "ADMIN");
        Pageable pageable = PageRequest.of(0, 10);
        when(claimRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        claimService.list(pageable);

        verify(claimRepository).findAll(pageable);
        verify(claimRepository, never()).findAllByInsurerId(any(), any());
    }

    @Test
    void getByIdReturnsOwnClaimForInsurerUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(insurerId));
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setInsurerId(insurerId);
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(insurerService.getById(insurerId)).thenReturn(insurerResponse());
        when(invoiceService.listByClaimId(id)).thenReturn(List.of());

        assertThat(claimService.getById(id).id()).isEqualTo(id);
    }

    @Test
    void getByIdRejectsOtherInsurersClaimForInsurerUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(UUID.randomUUID()));
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setInsurerId(insurerId);
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.getById(id))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByIdReturnsOwnClaimForProviderUser() {
        UUID organizationId = UUID.randomUUID();
        UUID serviceProviderId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(serviceProviderId));
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setServiceProviderId(serviceProviderId);
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(invoiceService.listByClaimId(id)).thenReturn(List.of());

        assertThat(claimService.getById(id).id()).isEqualTo(id);
    }

    @Test
    void getByIdRejectsOtherProvidersClaimForProviderUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(UUID.randomUUID()));
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setServiceProviderId(UUID.randomUUID());
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.getById(id))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listByVisitorPagedReturnsOnlyThatVisitorsClaims() {
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(UUID.randomUUID());
        claim.setVisitorId(visitorId);
        Pageable pageable = PageRequest.of(0, 10);
        when(claimRepository.findAllByVisitorId(visitorId, pageable)).thenReturn(new PageImpl<>(List.of(claim)));
        when(visitorService.getById(visitorId)).thenReturn(visitorResponse());
        when(invoiceService.listByClaimId(any())).thenReturn(List.of());

        Page<ClaimResponse> result = claimService.listByVisitor(visitorId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().visitorId()).isEqualTo(visitorId);
        verify(claimRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getByIdThrowsWhenUnknown() {
        UUID id = UUID.randomUUID();
        when(claimRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void decideApprovesWithinClaimedAmount() {
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(invoiceService.listByClaimId(id)).thenReturn(List.of());

        ClaimResponse response = claimService.decide(id,
                new ClaimDecisionRequest(ClaimStatus.APPROVED, new BigDecimal("40000.00"), "Approved"));

        assertThat(response.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(response.approvedAmount()).isEqualByComparingTo("40000.00");
        verify(eventPublisher).publish(any(), any());
    }

    @Test
    void decideRejectsAmountAboveClaimed() {
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.decide(id,
                new ClaimDecisionRequest(ClaimStatus.APPROVED, new BigDecimal("60000.00"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Approved amount cannot exceed claimed amount");
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void deleteRemovesClaim() {
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));

        claimService.delete(id);

        verify(claimRepository).delete(claim);
    }

    @Test
    void attachInvoiceAttachesToOpenClaimAndReturnsPopulatedInvoices() {
        UUID claimId = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(claimId);
        claim.setStatus(ClaimStatus.OPEN);

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(invoiceService.getEntityById(invoiceId)).thenReturn(invoiceWithBaseTotal("45000.00"));
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceService.listByClaimId(claimId)).thenReturn(List.of(invoiceResponse()));

        ClaimResponse response = claimService.attachInvoice(claimId, new AttachInvoiceRequest(invoiceId));

        assertThat(response.id()).isEqualTo(claimId);
        assertThat(response.status()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.invoices()).extracting(InvoiceResponse::invoiceNumber)
                .containsExactly("INV-2026-001");
        assertThat(response.claimedAmountBase()).isEqualByComparingTo("45000.00");
        verify(claimRepository).save(claim);
        verify(invoiceService).attachToClaim(invoiceId, claimId);
    }

    @Test
    void attachInvoiceAggregatesBaseAmountsAcrossAllInvoices() {
        UUID claimId = UUID.randomUUID();
        UUID secondInvoiceId = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(claimId);
        claim.setStatus(ClaimStatus.OPEN);
        claim.getInvoiceIds().add(secondInvoiceId);

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(invoiceService.getEntityById(invoiceId)).thenReturn(invoiceWithBaseTotal("100.00"));
        when(invoiceService.getEntityById(secondInvoiceId)).thenReturn(invoiceWithBaseTotal("250.00"));
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceService.listByClaimId(claimId)).thenReturn(List.of(invoiceResponse()));

        ClaimResponse response = claimService.attachInvoice(claimId, new AttachInvoiceRequest(invoiceId));

        assertThat(response.claimedAmountBase()).isEqualByComparingTo("350.00");
        verify(invoiceService).attachToClaim(invoiceId, claimId);
    }

    @Test
    void updateRecomputesBaseAmounts() {
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setStatus(ClaimStatus.OPEN);
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(benefitService.getEntityById(benefitId)).thenReturn(null);
        when(visitorService.getEntityById(visitorId)).thenReturn(visitorOnPolicy());
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(invoiceService.getEntityById(invoiceId)).thenReturn(invoiceWithBaseTotal("45000.00"));
        when(icd11CodeService.getEntityById(diagnosisId1)).thenReturn(null);
        when(icd11CodeService.getEntityById(diagnosisId2)).thenReturn(null);
        when(procedureService.getById(procedureId1)).thenReturn(procedureResponse(procedureId1));
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(insurerService.getById(insurerId)).thenReturn(insurerResponse());
        when(invoiceService.listByClaimId(id)).thenReturn(List.of(invoiceResponse()));

        ClaimResponse response = claimService.update(id, fullRequest());

        assertThat(response.claimedAmountBase()).isEqualByComparingTo("45000.00");
        assertThat(response.exchangeRate()).isEqualByComparingTo("0.0077");
        assertThat(response.invoices()).extracting(InvoiceResponse::invoiceNumber)
                .containsExactly("INV-2026-001");
        verify(claimRepository).save(claim);
    }

    @Test
    void attachInvoiceRejectsUnderReviewClaim() {
        UUID claimId = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(claimId);
        claim.setStatus(ClaimStatus.UNDER_REVIEW);

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.attachInvoice(claimId, new AttachInvoiceRequest(invoiceId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Claim is not open for attaching invoices: UNDER_REVIEW");
        verify(claimRepository, never()).save(any());
    }

    @Test
    void attachInvoiceThrowsConflictWhenClaimIsNotOpen() {
        UUID claimId = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(claimId);
        claim.setStatus(ClaimStatus.APPROVED);

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.attachInvoice(claimId, new AttachInvoiceRequest(invoiceId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Claim is not open for attaching invoices: APPROVED");
        verify(claimRepository, never()).save(any());
    }

    @Test
    void attachInvoiceThrowsNotFoundWhenClaimDoesNotExist() {
        UUID claimId = UUID.randomUUID();
        when(claimRepository.findById(claimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.attachInvoice(claimId, new AttachInvoiceRequest(invoiceId)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(claimRepository, never()).save(any());
    }

    @Test
    void attachInvoiceThrowsNotFoundWhenInvoiceDoesNotExist() {
        UUID claimId = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(claimId);
        claim.setStatus(ClaimStatus.OPEN);

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(invoiceService.getEntityById(invoiceId))
                .thenThrow(new ResourceNotFoundException("Invoice", invoiceId));

        assertThatThrownBy(() -> claimService.attachInvoice(claimId, new AttachInvoiceRequest(invoiceId)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(claimRepository, never()).save(any());
    }

    private PreauthorizationResponse preauthResponse(PreauthorizationStatus status, Boolean converted,
                                                     List<PreauthorizationItemResponse> items) {
        return new PreauthorizationResponse(preauthId, policyId, insurerId, visitorId, "Jane Traveler", "A1234567",
                diagnosisId1, "B54", "Malaria", benefitId, "Inpatient", serviceProviderId, "City Hospital",
                medicalServiceId, "Ward admission", new BigDecimal("60000.00"), new BigDecimal("50000.00"),
                "Admission for malaria", null, status, Instant.now(), Instant.now(), null, null,
                items, converted);
    }

    private PreauthorizationItemResponse preauthItem(String amount) {
        return new PreauthorizationItemResponse(UUID.randomUUID(), "Ward bed", new BigDecimal("2"),
                new BigDecimal(amount).divide(new BigDecimal("2")), new BigDecimal(amount), LocalDate.of(2026, 10, 1));
    }

    private final UUID preauthId = UUID.randomUUID();
    private final UUID serviceProviderId = UUID.randomUUID();
    private final UUID medicalServiceId = UUID.randomUUID();

    @Test
    void createFromPreauthorizationCopiesPreauthAndInvoicesItsItems() {
        when(preauthorizationService.getById(preauthId)).thenReturn(preauthResponse(
                PreauthorizationStatus.PARTIALLY_APPROVED, false,
                List.of(preauthItem("30000.00"), preauthItem("25000.00"))));
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimResponse response = claimService.createFromPreauthorization(
                preauthId, new ConvertPreauthorizationRequest(" INV-77 ", LocalDate.of(2026, 10, 2)));

        ArgumentCaptor<Claim> claimCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(claimCaptor.capture());
        Claim saved = claimCaptor.getValue();
        assertThat(saved.getPolicyId()).isEqualTo(policyId);
        assertThat(saved.getInsurerId()).isEqualTo(insurerId);
        assertThat(saved.getVisitorId()).isEqualTo(visitorId);
        assertThat(saved.getBenefitId()).isEqualTo(benefitId);
        assertThat(saved.getServiceProviderId()).isEqualTo(serviceProviderId);
        assertThat(saved.getPreauthorizationId()).isEqualTo(preauthId);
        assertThat(saved.getClaimedAmount()).isEqualByComparingTo("50000.00");
        assertThat(saved.getDescription()).isEqualTo("Admission for malaria");
        assertThat(saved.getDiagnosisIds()).containsExactly(diagnosisId1);
        assertThat(saved.getProcedureIds()).isEmpty();
        assertThat(saved.getStatus()).isEqualTo(ClaimStatus.OPEN);
        assertThat(response.preauthorizationId()).isEqualTo(preauthId);

        ArgumentCaptor<InvoiceRequest> invoiceCaptor = ArgumentCaptor.forClass(InvoiceRequest.class);
        verify(invoiceService).create(invoiceCaptor.capture());
        InvoiceRequest invoice = invoiceCaptor.getValue();
        assertThat(invoice.invoiceNumber()).isEqualTo("INV-77");
        assertThat(invoice.issueDate()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(invoice.currency()).isEqualTo("KES");
        assertThat(invoice.totalAmount()).isEqualByComparingTo("55000.00");
        assertThat(invoice.invoiceItems()).hasSize(2)
                .allSatisfy(item -> assertThat(item.medicalServiceId()).isEqualTo(medicalServiceId));

        verify(preauthorizationService).markPreAuthorizationConvertedToClaim(preauthId);
    }

    @Test
    void createFromPreauthorizationWithoutItemsInvoicesTheClaimedAmount() {
        when(preauthorizationService.getById(preauthId)).thenReturn(
                preauthResponse(PreauthorizationStatus.APPROVED, null, List.of()));
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        claimService.createFromPreauthorization(preauthId, new ConvertPreauthorizationRequest("INV-78", null));

        ArgumentCaptor<InvoiceRequest> invoiceCaptor = ArgumentCaptor.forClass(InvoiceRequest.class);
        verify(invoiceService).create(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().totalAmount()).isEqualByComparingTo("50000.00");
        assertThat(invoiceCaptor.getValue().invoiceItems()).isEmpty();
    }

    @Test
    void createFromPreauthorizationRejectsUndecidedPreauth() {
        when(preauthorizationService.getById(preauthId)).thenReturn(
                preauthResponse(PreauthorizationStatus.PENDING, false, List.of()));

        assertThatThrownBy(() -> claimService.createFromPreauthorization(
                preauthId, new ConvertPreauthorizationRequest("INV-79", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only approved");
        verify(claimRepository, never()).save(any());
        verify(invoiceService, never()).create(any());
    }

    @Test
    void createFromPreauthorizationRejectsAlreadyConvertedPreauth() {
        when(preauthorizationService.getById(preauthId)).thenReturn(
                preauthResponse(PreauthorizationStatus.APPROVED, true, List.of()));

        assertThatThrownBy(() -> claimService.createFromPreauthorization(
                preauthId, new ConvertPreauthorizationRequest("INV-80", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been converted");
        verify(claimRepository, never()).save(any());
        verify(invoiceService, never()).create(any());
    }

    @Test
    void decideStoresApprovedAmountInUsdUsingClaimedBaseRatio() {
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setClaimedAmount(new BigDecimal("50000.00"));
        claim.setClaimedAmountBase(new BigDecimal("385.00"));
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyService.getEntityById(policyId)).thenReturn(policyCoveredBy(insurerId));
        when(invoiceService.listByClaimId(id)).thenReturn(List.of());

        claimService.decide(id, new ClaimDecisionRequest(ClaimStatus.APPROVED, new BigDecimal("40000.00"), "ok"));

        assertThat(claim.getApprovedAmountBase()).isEqualByComparingTo("308.00");
    }

    @Test
    void decideRejectionStoresZeroApprovedAmountInUsd() {
        UUID id = UUID.randomUUID();
        Claim claim = claimMapper.toEntity(baseRequest());
        claim.setId(id);
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setClaimedAmount(new BigDecimal("50000.00"));
        claim.setClaimedAmountBase(new BigDecimal("385.00"));
        when(claimRepository.findById(id)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceService.listByClaimId(id)).thenReturn(List.of());

        claimService.decide(id, new ClaimDecisionRequest(ClaimStatus.REJECTED, null, "no cover"));

        assertThat(claim.getApprovedAmountBase()).isEqualByComparingTo("0.00");
    }

    private ClaimDashboardTotal dashboardTotal(UUID groupId, long count, String usd) {
        return new ClaimDashboardTotal() {
            @Override
            public UUID getGroupId() {
                return groupId;
            }

            @Override
            public long getClaimCount() {
                return count;
            }

            @Override
            public BigDecimal getApprovedAmountUsd() {
                return new BigDecimal(usd);
            }
        };
    }

    @Test
    void dashboardSummaryTotalsAllClaimsForAdmin() {
        authenticateAs(UUID.randomUUID(), "ADMIN");
        Instant from = LocalDate.of(2026, 10, 1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant to = LocalDate.of(2026, 11, 1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        when(claimRepository.summarize(ClaimStatus.APPROVED, null, null, from, to))
                .thenReturn(dashboardTotal(null, 7, "1234.50"));

        ClaimDashboardSummary summary = claimService.dashboardSummary(
                ClaimStatus.APPROVED, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(summary.totalClaims()).isEqualTo(7);
        assertThat(summary.totalApprovedAmountUsd()).isEqualByComparingTo("1234.50");
    }

    @Test
    void dashboardSummaryIsScopedToInsurerUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(insurerId));
        when(claimRepository.summarize(isNull(), eq(insurerId), isNull(), isNull(), isNull()))
                .thenReturn(dashboardTotal(null, 2, "10.00"));

        ClaimDashboardSummary summary = claimService.dashboardSummary(null, null, null);

        assertThat(summary.totalClaims()).isEqualTo(2);
    }

    @Test
    void dashboardSummaryIsScopedToProviderUser() {
        UUID organizationId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(providerId));
        when(claimRepository.summarize(isNull(), isNull(), eq(providerId), isNull(), isNull()))
                .thenReturn(dashboardTotal(null, 3, "20.00"));

        assertThat(claimService.dashboardSummary(null, null, null).totalClaims()).isEqualTo(3);
    }

    @Test
    void dashboardIsEmptyWhenInsurerUserHasNoInsurer() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.empty());

        assertThat(claimService.dashboardSummary(null, null, null).totalClaims()).isZero();
        assertThat(claimService.distributionByInsurer(null, null, null)).isEmpty();
        assertThat(claimService.distributionByServiceProvider(null, null, null)).isEmpty();
        verify(claimRepository, never()).summarize(any(), any(), any(), any(), any());
        verify(claimRepository, never()).summarizeByInsurer(any(), any(), any(), any(), any());
    }

    @Test
    void distributionByInsurerIncludesZeroClaimInsurersAndUnknownBucket() {
        authenticateAs(UUID.randomUUID(), "ADMIN");
        UUID quietInsurerId = UUID.randomUUID();
        when(insurerService.listAll()).thenReturn(List.of(
                insurerResponse(),
                new InsurerResponse(quietInsurerId, null, "Quiet Insurance", null, null, null, null,
                        null, null, null, null, null, null, null, Instant.now(), Instant.now())));
        when(claimRepository.summarizeByInsurer(null, null, null, null, null)).thenReturn(List.of(
                dashboardTotal(insurerId, 4, "100.00"), dashboardTotal(null, 1, "5.00")));

        List<InsurerClaimDistribution> result = claimService.distributionByInsurer(null, null, null);

        assertThat(result).extracting(InsurerClaimDistribution::insurerName)
                .containsExactly("Jubilee Insurance", "Quiet Insurance", "Unknown");
        assertThat(result).extracting(InsurerClaimDistribution::claimCount).containsExactly(4L, 0L, 1L);
        assertThat(result.get(1).approvedAmountUsd()).isEqualByComparingTo("0");
        assertThat(result.get(2).insurerId()).isNull();
    }

    @Test
    void distributionByInsurerShowsInsurerUserOnlyTheirOwnRow() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(insurerId));
        when(insurerService.listAll()).thenReturn(List.of(
                insurerResponse(),
                new InsurerResponse(UUID.randomUUID(), null, "Other Insurance", null, null, null, null,
                        null, null, null, null, null, null, null, Instant.now(), Instant.now())));
        when(claimRepository.summarizeByInsurer(null, insurerId, null, null, null))
                .thenReturn(List.of(dashboardTotal(insurerId, 4, "100.00")));

        List<InsurerClaimDistribution> result = claimService.distributionByInsurer(null, null, null);

        assertThat(result).extracting(InsurerClaimDistribution::insurerName).containsExactly("Jubilee Insurance");
    }

    @Test
    void distributionByServiceProviderIncludesZeroClaimProvidersAndUnknownBucket() {
        authenticateAs(UUID.randomUUID(), "ADMIN");
        UUID busyId = UUID.randomUUID();
        UUID quietId = UUID.randomUUID();
        when(serviceProviderService.listAll()).thenReturn(List.of(
                new ServiceProviderResponse(busyId, "Aga Khan Hospital", null, null, null, null, null, null, null,
                        Instant.now(), Instant.now()),
                new ServiceProviderResponse(quietId, "Quiet Clinic", null, null, null, null, null, null, null,
                        Instant.now(), Instant.now())));
        when(claimRepository.summarizeByServiceProvider(null, null, null, null, null)).thenReturn(List.of(
                dashboardTotal(busyId, 9, "900.00"), dashboardTotal(null, 2, "0.00")));

        List<ServiceProviderClaimDistribution> result =
                claimService.distributionByServiceProvider(null, null, null);

        assertThat(result).extracting(ServiceProviderClaimDistribution::serviceProviderName)
                .containsExactly("Aga Khan Hospital", "Quiet Clinic", "Unknown");
        assertThat(result).extracting(ServiceProviderClaimDistribution::claimCount).containsExactly(9L, 0L, 2L);
    }

    @Test
    void distributionByServiceProviderShowsProviderUserOnlyTheirOwnRow() {
        UUID organizationId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(providerId));
        when(serviceProviderService.listAll()).thenReturn(List.of(
                new ServiceProviderResponse(providerId, "Mine", null, null, null, null, null, null, null,
                        Instant.now(), Instant.now()),
                new ServiceProviderResponse(UUID.randomUUID(), "Theirs", null, null, null, null, null, null, null,
                        Instant.now(), Instant.now())));
        when(claimRepository.summarizeByServiceProvider(null, null, providerId, null, null))
                .thenReturn(List.of(dashboardTotal(providerId, 1, "10.00")));

        assertThat(claimService.distributionByServiceProvider(null, null, null))
                .extracting(ServiceProviderClaimDistribution::serviceProviderName).containsExactly("Mine");
    }
}
