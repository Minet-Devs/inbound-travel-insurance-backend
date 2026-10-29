package com.travel.insurance.preauthorization;

import java.util.Set;
import java.time.ZoneOffset;
import com.travel.insurance.preauthorization.dto.ServiceProviderPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.PreauthorizationDashboardSummary;
import com.travel.insurance.preauthorization.dto.InsurerPreauthorizationDistribution;
import com.travel.insurance.insurer.dto.InsurerResponse;
import org.mockito.ArgumentCaptor;
import com.travel.insurance.preauthorization.dto.OfflineApproval;
import com.travel.insurance.common.domain.VisitType;
import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.exception.ExchangeRateUnavailableException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.common.service.CurrencyConversionService;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.config.RabbitConfig;
import com.travel.insurance.icd11.Icd11Code;
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.medicalservice.MedicalServiceService;
import com.travel.insurance.medicalservice.dto.MedicalServiceResponse;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.policy.PolicyStatus;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationItemRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreauthorizationServiceImplTest {

    @Mock
    private PreauthorizationRepository preauthorizationRepository;

    @Mock
    private PreauthorizationEnhancementRepository preauthorizationEnhancementRepository;

    @Mock
    private PreauthorizationItemRepository preauthorizationItemRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private BenefitService benefitService;

    @Mock
    private VisitorService visitorService;

    @Mock
    private Icd11CodeService icd11CodeService;

    @Mock
    private ServiceProviderService serviceProviderService;

    @Mock
    private InsurerService insurerService;

    @Mock
    private MedicalServiceService medicalServiceService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private CurrencyConversionService currencyConversionService;

    private final PreauthorizationMapper preauthorizationMapper = new PreauthorizationMapper();

    private PreauthorizationServiceImpl preauthorizationService;

    private final UUID policyId = UUID.randomUUID();
    private final UUID insurerId = UUID.randomUUID();
    private final UUID visitorId = UUID.randomUUID();
    private final UUID icd11CodeId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();
    private final UUID serviceProviderId = UUID.randomUUID();
    private final UUID medicalServiceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        preauthorizationService = new PreauthorizationServiceImpl(
                preauthorizationRepository, preauthorizationEnhancementRepository, preauthorizationItemRepository,
                preauthorizationMapper, policyService, benefitService, visitorService, icd11CodeService,
                serviceProviderService, insurerService, medicalServiceService, eventPublisher,
                currencyConversionService);

        lenient().when(policyService.getEntityById(policyId)).thenReturn(activePolicy());
        lenient().when(visitorService.getEntityById(visitorId)).thenReturn(visitor());
        lenient().when(icd11CodeService.getEntityById(icd11CodeId)).thenReturn(icd11Code());
        lenient().when(benefitService.getEntityById(benefitId)).thenReturn(benefit());
        lenient().when(serviceProviderService.getById(serviceProviderId)).thenReturn(serviceProviderResponse());
        lenient().when(preauthorizationEnhancementRepository.save(any(PreauthorizationEnhancement.class)))
                .thenAnswer(invocation -> {
                    PreauthorizationEnhancement enhancement = invocation.getArgument(0);
                    enhancement.setId(UUID.randomUUID());
                    return enhancement;
                });
        lenient().when(preauthorizationEnhancementRepository.findByPreauthorizationId(any()))
                .thenReturn(Optional.empty());
        lenient().when(preauthorizationItemRepository.findAllByEnhancementId(any())).thenReturn(List.of());
        lenient().when(preauthorizationItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Policy activePolicy() {
        Policy policy = new Policy();
        policy.setId(policyId);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setInsurerId(insurerId);
        return policy;
    }

    private Visitor visitor() {
        Visitor visitor = new Visitor();
        visitor.setId(visitorId);
        visitor.setPolicyId(policyId);
        visitor.setFullName("Jane Traveler");
        visitor.setPassportNumber("A1234567");
        return visitor;
    }

    private Icd11Code icd11Code() {
        Icd11Code icd11Code = new Icd11Code();
        icd11Code.setId(icd11CodeId);
        icd11Code.setCode("1A00");
        icd11Code.setTitle("Cholera");
        return icd11Code;
    }

    private Benefit benefit() {
        Benefit benefit = new Benefit();
        benefit.setId(benefitId);
        benefit.setBenefitName("Medical Expenses");
        benefit.setLimitAmount(new BigDecimal("20000.00"));
        return benefit;
    }

    private ServiceProviderResponse serviceProviderResponse() {
        return new ServiceProviderResponse(serviceProviderId, "Aga Khan Hospital",
                "contact@agakhan.co.ke", null, null, null, null, null, null, Instant.now(), Instant.now());
    }

    private MedicalServiceResponse medicalServiceResponse() {
        return new MedicalServiceResponse(medicalServiceId, "Malaria Test", UUID.randomUUID(),
                "Laboratory", Instant.now(), Instant.now());
    }

    private PreauthorizationRequest validRequest() {
        return new PreauthorizationRequest(
                policyId, visitorId, icd11CodeId, benefitId, serviceProviderId, null,
                new BigDecimal("500.00"), "X-ray", null, VisitType.INPATIENT);
    }

    private Preauthorization pendingPreauthorization() {
        Preauthorization preauthorization = new Preauthorization();
        preauthorization.setId(UUID.randomUUID());
        preauthorization.setPolicyId(policyId);
        preauthorization.setInsurerId(insurerId);
        preauthorization.setVisitorId(visitorId);
        preauthorization.setIcd11CodeId(icd11CodeId);
        preauthorization.setBenefitId(benefitId);
        preauthorization.setServiceProviderId(serviceProviderId);
        preauthorization.setRequestedAmount(new BigDecimal("500.00"));
        preauthorization.setStatus(PreauthorizationStatus.PENDING);
        return preauthorization;
    }

    private Preauthorization legacyPreauthorizationWithoutVisitorOrDiagnosis() {
        Preauthorization preauthorization = pendingPreauthorization();
        preauthorization.setVisitorId(null);
        preauthorization.setIcd11CodeId(null);
        return preauthorization;
    }

    private void authenticateAs(UUID organizationId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), organizationId, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private record StatusCountRow(PreauthorizationStatus status, long count) implements PreauthorizationStatusCount {
        public PreauthorizationStatus getStatus() {
            return status;
        }

        public long getStatusCount() {
            return count;
        }
    }

    private record DashboardTotalRow(UUID groupId, long count, BigDecimal amount)
            implements PreauthorizationDashboardTotal {
        public UUID getGroupId() {
            return groupId;
        }

        public long getPreauthorizationCount() {
            return count;
        }

        public BigDecimal getApprovedAmount() {
            return amount;
        }
    }

    private PreauthorizationStatusCount statusCount(PreauthorizationStatus status, long count) {
        return new StatusCountRow(status, count);
    }

    private PreauthorizationDashboardTotal dashboardTotal(UUID groupId, long count, String amount) {
        return new DashboardTotalRow(groupId, count, new BigDecimal(amount));
    }

    @Test
    void dashboardSummaryCountsEachStatusAndTotal() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        when(preauthorizationRepository.countByStatus(null, null,
                from.atStartOfDay().toInstant(ZoneOffset.UTC), to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)))
                .thenReturn(List.of(
                        statusCount(PreauthorizationStatus.APPROVED, 5),
                        statusCount(PreauthorizationStatus.REJECTED, 2),
                        statusCount(PreauthorizationStatus.PARTIALLY_APPROVED, 1),
                        statusCount(PreauthorizationStatus.PENDING, 3)));

        PreauthorizationDashboardSummary summary = preauthorizationService.dashboardSummary(from, to);

        assertThat(summary.totalPreauthorizations()).isEqualTo(11);
        assertThat(summary.approved()).isEqualTo(5);
        assertThat(summary.rejected()).isEqualTo(2);
        assertThat(summary.partiallyApproved()).isEqualTo(1);
    }

    @Test
    void dashboardSummaryIsZeroWhenStatusesAreAbsent() {
        when(preauthorizationRepository.countByStatus(null, null, null, null)).thenReturn(List.of());

        PreauthorizationDashboardSummary summary = preauthorizationService.dashboardSummary(null, null);

        assertThat(summary).isEqualTo(new PreauthorizationDashboardSummary(0, 0, 0, 0));
    }

    @Test
    void dashboardScopesInsurerUserToOwnInsurerAndEmptyWhenUnresolvable() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId))
                .thenReturn(Optional.of(insurerId)).thenReturn(Optional.empty());
        when(preauthorizationRepository.countByStatus(insurerId, null, null, null))
                .thenReturn(List.of(statusCount(PreauthorizationStatus.APPROVED, 4)));

        assertThat(preauthorizationService.dashboardSummary(null, null).approved()).isEqualTo(4);
        assertThat(preauthorizationService.dashboardSummary(null, null))
                .isEqualTo(new PreauthorizationDashboardSummary(0, 0, 0, 0));
        verify(preauthorizationRepository, never()).countByStatus(eq(null), eq(null), any(), any());
    }

    @Test
    void distributionByInsurerListsEveryInsurerIncludingThoseWithNone() {
        UUID otherInsurerId = UUID.randomUUID();
        when(insurerService.listAll()).thenReturn(List.of(
                insurerResponse(insurerId, "Jubilee Insurance"), insurerResponse(otherInsurerId, "Pioneer")));
        when(preauthorizationRepository.summarizeByInsurer(
                Set.of(PreauthorizationStatus.APPROVED, PreauthorizationStatus.PARTIALLY_APPROVED),
                null, null, null, null))
                .thenReturn(List.of(dashboardTotal(insurerId, 3, "1500.00")));

        List<InsurerPreauthorizationDistribution> result = preauthorizationService.distributionByInsurer(null, null);

        assertThat(result).containsExactly(
                new InsurerPreauthorizationDistribution(insurerId, "Jubilee Insurance", 3, new BigDecimal("1500.00")),
                new InsurerPreauthorizationDistribution(otherInsurerId, "Pioneer", 0, BigDecimal.ZERO));
    }

    @Test
    void topServiceProvidersOrdersByCountThenAmountAndLimitsToTenByDefault() {
        List<PreauthorizationDashboardTotal> totals = new java.util.ArrayList<>();
        java.util.Map<UUID, String> names = new java.util.HashMap<>();
        for (int i = 0; i < 12; i++) {
            UUID id = UUID.randomUUID();
            totals.add(dashboardTotal(id, 20 - i, "100.00"));
            names.put(id, "Provider " + i);
        }
        UUID tieWinner = UUID.randomUUID();
        totals.add(dashboardTotal(tieWinner, 20, "999.00"));
        names.put(tieWinner, "Tie winner");
        when(preauthorizationRepository.summarizeByServiceProvider(
                Set.of(PreauthorizationStatus.APPROVED, PreauthorizationStatus.PARTIALLY_APPROVED),
                null, null, null, null)).thenReturn(totals);
        when(serviceProviderService.namesByIds(any())).thenReturn(names);

        List<ServiceProviderPreauthorizationDistribution> result =
                preauthorizationService.topServiceProviders(null, null, null);

        assertThat(result).hasSize(10);
        assertThat(result.get(0).serviceProviderName()).isEqualTo("Tie winner");
        assertThat(result.get(1).serviceProviderName()).isEqualTo("Provider 0");
        assertThat(result.get(9).preauthorizationCount()).isEqualTo(12);
    }

    @Test
    void topServiceProvidersRejectsNonPositiveLimit() {
        assertThatThrownBy(() -> preauthorizationService.topServiceProviders(null, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    private InsurerResponse insurerResponse(UUID id, String name) {
        return new InsurerResponse(id, policyId, name, null, null, null, null,
                null, null, null, null, null, null, null, Instant.now(), Instant.now());
    }

    @Test
    void createRejectsWhenPolicyIsNotActive() {
        Policy draftPolicy = new Policy();
        draftPolicy.setId(policyId);
        draftPolicy.setStatus(PolicyStatus.DRAFT);
        when(policyService.getEntityById(policyId)).thenReturn(draftPolicy);

        assertThatThrownBy(() -> preauthorizationService.create(validRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(policyId.toString());
        verify(preauthorizationRepository, never()).save(any());
    }

    @Test
    void createValidatesIcd11CodeExists() {
        when(icd11CodeService.getEntityById(icd11CodeId))
                .thenThrow(new ResourceNotFoundException("Icd11Code", icd11CodeId));

        assertThatThrownBy(() -> preauthorizationService.create(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(preauthorizationRepository, never()).save(any());
    }

    @Test
    void createValidatesServiceProviderExists() {
        when(serviceProviderService.getById(serviceProviderId))
                .thenThrow(new ResourceNotFoundException("ServiceProvider", serviceProviderId));

        assertThatThrownBy(() -> preauthorizationService.create(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(preauthorizationRepository, never()).save(any());
    }

    @Test
    void createValidatesMedicalServiceWhenProvided() {
        when(medicalServiceService.getById(medicalServiceId))
                .thenThrow(new ResourceNotFoundException("MedicalService", medicalServiceId));

        PreauthorizationRequest request = new PreauthorizationRequest(
                policyId, visitorId, icd11CodeId, benefitId, serviceProviderId, medicalServiceId,
                new BigDecimal("500.00"), "X-ray", null, null);

        assertThatThrownBy(() -> preauthorizationService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(preauthorizationRepository, never()).save(any());
    }

    @Test
    void createSavesPreauthorizationWhenAllReferencesAreValid() {
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreauthorizationResponse response = preauthorizationService.create(validRequest());

        assertThat(response.status()).isEqualTo(PreauthorizationStatus.PENDING);
        assertThat(response.insurerId()).isEqualTo(insurerId);
        assertThat(response.visitorId()).isEqualTo(visitorId);
        assertThat(response.visitorName()).isEqualTo("Jane Traveler");
        assertThat(response.passportNumber()).isEqualTo("A1234567");
        assertThat(response.icd11Code()).isEqualTo("1A00");
        assertThat(response.icd11Title()).isEqualTo("Cholera");
        assertThat(response.benefitName()).isEqualTo("Medical Expenses");
        assertThat(response.serviceProviderName()).isEqualTo("Aga Khan Hospital");
        assertThat(response.requestedAmount()).isEqualByComparingTo("500.00");
        assertThat(response.visitType()).isEqualTo(VisitType.INPATIENT);
        assertThat(response.decidedBy()).isNull();
        assertThat(response.decidedAt()).isNull();
        verify(preauthorizationRepository).save(any(Preauthorization.class));
        verify(preauthorizationEnhancementRepository).save(any(PreauthorizationEnhancement.class));
    }

    @Test
    void createStoresKesToUsdExchangeRate() {
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(new BigDecimal("0.007752"));
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreauthorizationResponse response = preauthorizationService.create(validRequest());

        assertThat(response.exchangeRate()).isEqualByComparingTo("0.007752");
    }

    @Test
    void createLeavesExchangeRateNullWhenRateServiceUnavailable() {
        when(currencyConversionService.getExchangeRate("KES", "USD"))
                .thenThrow(new ExchangeRateUnavailableException("down"));
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreauthorizationResponse response = preauthorizationService.create(validRequest());

        assertThat(response.exchangeRate()).isNull();
        assertThat(response.status()).isEqualTo(PreauthorizationStatus.PENDING);
    }

    @Test
    void createOfflineApprovalSavesApprovedPreauthorizationWithDates() {
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> {
                    Preauthorization saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        UUID id = preauthorizationService.createOfflineApproval(new OfflineApproval(
                visitorId, benefitId, serviceProviderId, new BigDecimal("1234.50"), VisitType.OUTPATIENT,
                LocalDate.of(2026, 10, 10), null));

        ArgumentCaptor<Preauthorization> captor = ArgumentCaptor.forClass(Preauthorization.class);
        verify(preauthorizationRepository).save(captor.capture());
        Preauthorization saved = captor.getValue();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getStatus()).isEqualTo(PreauthorizationStatus.APPROVED);
        assertThat(saved.getPolicyId()).isEqualTo(policyId);
        assertThat(saved.getInsurerId()).isEqualTo(insurerId);
        assertThat(saved.getRequestedAmount()).isEqualByComparingTo("1234.50");
        assertThat(saved.getApprovedAmount()).isEqualByComparingTo("1234.50");
        assertThat(saved.getVisitType()).isEqualTo(VisitType.OUTPATIENT);
        assertThat(saved.getTreatmentDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(saved.getDischargeDate()).isNull();
        assertThat(saved.getIcd11CodeId()).isNull();
    }

    @Test
    void createOfflineApprovalStoresExchangeRate() {
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(new BigDecimal("0.007752"));
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        preauthorizationService.createOfflineApproval(new OfflineApproval(
                visitorId, benefitId, serviceProviderId, new BigDecimal("100.00"), VisitType.OUTPATIENT,
                LocalDate.of(2026, 10, 10), null));

        ArgumentCaptor<Preauthorization> captor = ArgumentCaptor.forClass(Preauthorization.class);
        verify(preauthorizationRepository).save(captor.capture());
        assertThat(captor.getValue().getExchangeRate()).isEqualByComparingTo("0.007752");
        assertThat(captor.getValue().getApprovedAmountInDollars()).isEqualByComparingTo("0.78");
    }

    @Test
    void createOfflineApprovalRejectsDuplicate() {
        when(preauthorizationRepository
                .existsByVisitorIdAndServiceProviderIdAndBenefitIdAndTreatmentDateAndRequestedAmount(
                        visitorId, serviceProviderId, benefitId, LocalDate.of(2026, 10, 10),
                        new BigDecimal("1234.50"))).thenReturn(true);

        assertThatThrownBy(() -> preauthorizationService.createOfflineApproval(new OfflineApproval(
                visitorId, benefitId, serviceProviderId, new BigDecimal("1234.50"), null,
                LocalDate.of(2026, 10, 10), null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        verify(preauthorizationRepository, never()).save(any());
    }

    @Test
    void createPersistsEnhancementAndLineItemsInOneCall() {
        when(medicalServiceService.getById(medicalServiceId)).thenReturn(medicalServiceResponse());
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreauthorizationItemRequest itemRequest = new PreauthorizationItemRequest(
                "Malaria test (rapid)", new BigDecimal("1"), new BigDecimal("500.00"),
                new BigDecimal("500.00"), LocalDate.of(2026, 8, 10));
        PreauthorizationRequest request = new PreauthorizationRequest(
                policyId, visitorId, icd11CodeId, benefitId, serviceProviderId, medicalServiceId,
                new BigDecimal("500.00"), "X-ray", List.of(itemRequest), null);

        UUID savedEnhancementId = UUID.randomUUID();
        PreauthorizationEnhancement savedEnhancement = preauthorizationMapper.toEnhancement(UUID.randomUUID(), request);
        savedEnhancement.setId(savedEnhancementId);
        when(preauthorizationEnhancementRepository.save(any(PreauthorizationEnhancement.class)))
                .thenReturn(savedEnhancement);
        when(preauthorizationEnhancementRepository.findByPreauthorizationId(any()))
                .thenReturn(Optional.of(savedEnhancement));
        PreauthorizationItem savedItem = preauthorizationMapper.toItem(itemRequest, savedEnhancementId);
        when(preauthorizationItemRepository.findAllByEnhancementId(savedEnhancementId)).thenReturn(List.of(savedItem));

        PreauthorizationResponse response = preauthorizationService.create(request);

        assertThat(response.medicalServiceId()).isEqualTo(medicalServiceId);
        assertThat(response.medicalServiceName()).isEqualTo("Malaria Test");
        assertThat(response.preauthorizationItems()).hasSize(1);
        assertThat(response.preauthorizationItems().getFirst().description()).isEqualTo("Malaria test (rapid)");
        verify(preauthorizationItemRepository).saveAll(any());
    }

    @Test
    void getByIdToleratesLegacyRowsMissingVisitorAndDiagnosis() {
        Preauthorization legacy = legacyPreauthorizationWithoutVisitorOrDiagnosis();
        when(preauthorizationRepository.findById(legacy.getId())).thenReturn(Optional.of(legacy));

        PreauthorizationResponse response = preauthorizationService.getById(legacy.getId());

        assertThat(response.visitorId()).isNull();
        assertThat(response.visitorName()).isNull();
        assertThat(response.passportNumber()).isNull();
        assertThat(response.icd11CodeId()).isNull();
        assertThat(response.icd11Code()).isNull();
        assertThat(response.icd11Title()).isNull();
        assertThat(response.medicalServiceId()).isNull();
        assertThat(response.medicalServiceName()).isNull();
        assertThat(response.preauthorizationItems()).isEmpty();
        assertThat(response.benefitName()).isEqualTo("Medical Expenses");
    }

    @Test
    void decideRejectsAlreadyDecidedPreauthorization() {
        Preauthorization decided = pendingPreauthorization();
        decided.setStatus(PreauthorizationStatus.APPROVED);
        when(preauthorizationRepository.findById(decided.getId())).thenReturn(Optional.of(decided));

        PreauthorizationDecisionRequest request = new PreauthorizationDecisionRequest(
                PreauthorizationStatus.REJECTED, null, "duplicate");

        assertThatThrownBy(() -> preauthorizationService.decide(decided.getId(), request))
                .isInstanceOf(IllegalStateException.class);
        verify(preauthorizationRepository, never()).save(any());
    }

    @Test
    void decideRejectsNonDecisionStatus() {
        Preauthorization pending = pendingPreauthorization();
        when(preauthorizationRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        PreauthorizationDecisionRequest request = new PreauthorizationDecisionRequest(
                PreauthorizationStatus.PENDING, null, null);

        assertThatThrownBy(() -> preauthorizationService.decide(pending.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decideRejectsApprovedAmountAboveRequestedAmount() {
        Preauthorization pending = pendingPreauthorization();
        when(preauthorizationRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        PreauthorizationDecisionRequest request = new PreauthorizationDecisionRequest(
                PreauthorizationStatus.APPROVED, new BigDecimal("999.00"), "approved");

        assertThatThrownBy(() -> preauthorizationService.decide(pending.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(preauthorizationRepository, never()).save(any());
    }

    @Test
    void decideDefaultsApprovedAmountToRequestedAmountAndPublishesEvent() {
        Preauthorization pending = pendingPreauthorization();
        when(preauthorizationRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreauthorizationDecisionRequest request = new PreauthorizationDecisionRequest(
                PreauthorizationStatus.APPROVED, null, "approved in full");

        PreauthorizationResponse response = preauthorizationService.decide(pending.getId(), request);

        assertThat(response.approvedAmount()).isEqualByComparingTo("500.00");
        verify(eventPublisher).publish(eq(RabbitConfig.PREAUTHORIZATION_DECIDED_KEY), any());
    }

    @Test
    void decideComputesApprovedAmountInDollarsFromExchangeRate() {
        Preauthorization pending = pendingPreauthorization();
        pending.setExchangeRate(new BigDecimal("0.007702"));
        when(preauthorizationRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreauthorizationResponse response = preauthorizationService.decide(pending.getId(),
                new PreauthorizationDecisionRequest(PreauthorizationStatus.APPROVED, new BigDecimal("400.00"), "ok"));

        assertThat(response.approvedAmountInDollars()).isEqualByComparingTo("3.08");
    }

    @Test
    void decideLeavesApprovedAmountInDollarsNullWithoutExchangeRate() {
        Preauthorization pending = pendingPreauthorization();
        pending.setExchangeRate(null);
        when(preauthorizationRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreauthorizationResponse response = preauthorizationService.decide(pending.getId(),
                new PreauthorizationDecisionRequest(PreauthorizationStatus.APPROVED, null, "ok"));

        assertThat(response.approvedAmountInDollars()).isNull();
    }

    @Test
    void decideSetsApprovedAmountToZeroWhenRejected() {
        Preauthorization pending = pendingPreauthorization();
        when(preauthorizationRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(preauthorizationRepository.save(any(Preauthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreauthorizationDecisionRequest request = new PreauthorizationDecisionRequest(
                PreauthorizationStatus.REJECTED, null, "not covered");

        PreauthorizationResponse response = preauthorizationService.decide(pending.getId(), request);

        assertThat(response.approvedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void listScopesToServiceProviderForProviderUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(serviceProviderId));
        Page<Preauthorization> page = new PageImpl<>(List.of(pendingPreauthorization()));
        Pageable pageable = PageRequest.of(0, 10);
        when(preauthorizationRepository.findAllByServiceProviderId(serviceProviderId, pageable)).thenReturn(page);

        Page<PreauthorizationResponse> result = preauthorizationService.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().serviceProviderName()).isEqualTo("Aga Khan Hospital");
        verify(preauthorizationRepository).findAllByServiceProviderId(serviceProviderId, pageable);
        verify(preauthorizationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listScopesToOwnInsurerForInsurerUserIgnoringInsurerIdParam() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(insurerId));
        Pageable pageable = PageRequest.of(0, 10);
        when(preauthorizationRepository.findAllByInsurerId(insurerId, pageable))
                .thenReturn(new PageImpl<>(List.of(pendingPreauthorization())));

        Page<PreauthorizationResponse> result = preauthorizationService.list(UUID.randomUUID(), pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(preauthorizationRepository).findAllByInsurerId(insurerId, pageable);
        verify(preauthorizationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listReturnsEmptyForInsurerUserWithoutInsurer() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.empty());

        Page<PreauthorizationResponse> result = preauthorizationService.list(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        verify(preauthorizationRepository, never()).findAll(any(Pageable.class));
        verify(preauthorizationRepository, never()).findAllByInsurerId(any(), any());
    }

    @Test
    void getByIdReturnsOwnPreauthorizationForInsurerUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(insurerId));
        Preauthorization preauthorization = pendingPreauthorization();
        when(preauthorizationRepository.findById(preauthorization.getId())).thenReturn(Optional.of(preauthorization));

        assertThat(preauthorizationService.getById(preauthorization.getId()).id()).isEqualTo(preauthorization.getId());
    }

    @Test
    void getByIdRejectsOtherInsurersPreauthorizationForInsurerUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "INSURER_USER");
        when(insurerService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(UUID.randomUUID()));
        Preauthorization preauthorization = pendingPreauthorization();
        when(preauthorizationRepository.findById(preauthorization.getId())).thenReturn(Optional.of(preauthorization));

        assertThatThrownBy(() -> preauthorizationService.getById(preauthorization.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByIdReturnsOwnPreauthorizationForProviderUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(serviceProviderId));
        Preauthorization preauthorization = pendingPreauthorization();
        when(preauthorizationRepository.findById(preauthorization.getId())).thenReturn(Optional.of(preauthorization));

        assertThat(preauthorizationService.getById(preauthorization.getId()).id()).isEqualTo(preauthorization.getId());
    }

    @Test
    void getByIdRejectsOtherProvidersPreauthorizationForProviderUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(UUID.randomUUID()));
        Preauthorization preauthorization = pendingPreauthorization();
        when(preauthorizationRepository.findById(preauthorization.getId())).thenReturn(Optional.of(preauthorization));

        assertThatThrownBy(() -> preauthorizationService.getById(preauthorization.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listByVisitorReturnsOnlyThatVisitorsPreauthorizations() {
        UUID visitorId = UUID.randomUUID();
        Page<Preauthorization> page = new PageImpl<>(List.of(pendingPreauthorization()));
        Pageable pageable = PageRequest.of(0, 10);
        when(preauthorizationRepository.findAllByVisitorId(visitorId, pageable)).thenReturn(page);

        Page<PreauthorizationResponse> result = preauthorizationService.listByVisitor(visitorId, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(preauthorizationRepository).findAllByVisitorId(visitorId, pageable);
        verify(preauthorizationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listReturnsAllForNonProviderUser() {
        authenticateAs(UUID.randomUUID(), "ADMIN");
        Page<Preauthorization> page = new PageImpl<>(List.of(pendingPreauthorization()));
        Pageable pageable = PageRequest.of(0, 10);
        when(preauthorizationRepository.findAll(pageable)).thenReturn(page);

        Page<PreauthorizationResponse> result = preauthorizationService.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(preauthorizationRepository, never()).findAllByServiceProviderId(any(), any());
    }

    @Test
    void listFiltersByInsurerIdForNonProviderUser() {
        authenticateAs(UUID.randomUUID(), "ADMIN");
        Page<Preauthorization> page = new PageImpl<>(List.of(pendingPreauthorization()));
        Pageable pageable = PageRequest.of(0, 10);
        when(preauthorizationRepository.findAllByInsurerId(insurerId, pageable)).thenReturn(page);

        Page<PreauthorizationResponse> result = preauthorizationService.list(insurerId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().insurerId()).isEqualTo(insurerId);
        verify(preauthorizationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listCombinesInsurerIdFilterWithServiceProviderScopeForProviderUser() {
        UUID organizationId = UUID.randomUUID();
        authenticateAs(organizationId, "PROVIDER_USER");
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(serviceProviderId));
        Page<Preauthorization> page = new PageImpl<>(List.of(pendingPreauthorization()));
        Pageable pageable = PageRequest.of(0, 10);
        when(preauthorizationRepository.findAllByServiceProviderIdAndInsurerId(serviceProviderId, insurerId, pageable))
                .thenReturn(page);

        Page<PreauthorizationResponse> result = preauthorizationService.list(insurerId, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(preauthorizationRepository).findAllByServiceProviderIdAndInsurerId(serviceProviderId, insurerId, pageable);
        verify(preauthorizationRepository, never()).findAllByServiceProviderId(any(), any());
    }
}