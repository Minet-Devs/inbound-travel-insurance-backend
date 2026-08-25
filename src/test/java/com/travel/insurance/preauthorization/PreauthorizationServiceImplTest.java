package com.travel.insurance.preauthorization;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.config.RabbitConfig;
import com.travel.insurance.icd11.Icd11Code;
import com.travel.insurance.icd11.Icd11CodeService;
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
    private MedicalServiceService medicalServiceService;

    @Mock
    private EventPublisher eventPublisher;

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
                serviceProviderService, medicalServiceService, eventPublisher);

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
                "contact@agakhan.co.ke", null, null, null, Instant.now(), Instant.now());
    }

    private MedicalServiceResponse medicalServiceResponse() {
        return new MedicalServiceResponse(medicalServiceId, "Malaria Test", UUID.randomUUID(),
                "Laboratory", Instant.now(), Instant.now());
    }

    private PreauthorizationRequest validRequest() {
        return new PreauthorizationRequest(
                policyId, visitorId, icd11CodeId, benefitId, serviceProviderId, null,
                new BigDecimal("500.00"), "X-ray", null);
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
                new BigDecimal("500.00"), "X-ray", null);

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
        assertThat(response.icd11Code()).isEqualTo("1A00");
        assertThat(response.icd11Title()).isEqualTo("Cholera");
        assertThat(response.benefitName()).isEqualTo("Medical Expenses");
        assertThat(response.serviceProviderName()).isEqualTo("Aga Khan Hospital");
        assertThat(response.requestedAmount()).isEqualByComparingTo("500.00");
        assertThat(response.decidedBy()).isNull();
        assertThat(response.decidedAt()).isNull();
        verify(preauthorizationRepository).save(any(Preauthorization.class));
        verify(preauthorizationEnhancementRepository).save(any(PreauthorizationEnhancement.class));
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
                new BigDecimal("500.00"), "X-ray", List.of(itemRequest));

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

        Page<PreauthorizationResponse> result = preauthorizationService.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().serviceProviderName()).isEqualTo("Aga Khan Hospital");
        verify(preauthorizationRepository).findAllByServiceProviderId(serviceProviderId, pageable);
        verify(preauthorizationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listReturnsAllForNonProviderUser() {
        authenticateAs(UUID.randomUUID(), "ADMIN");
        Page<Preauthorization> page = new PageImpl<>(List.of(pendingPreauthorization()));
        Pageable pageable = PageRequest.of(0, 10);
        when(preauthorizationRepository.findAll(pageable)).thenReturn(page);

        Page<PreauthorizationResponse> result = preauthorizationService.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(preauthorizationRepository, never()).findAllByServiceProviderId(any(), any());
    }
}