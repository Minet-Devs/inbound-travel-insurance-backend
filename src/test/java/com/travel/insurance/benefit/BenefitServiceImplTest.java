package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitServiceImplTest {

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private PolicyService policyService;

    private final BenefitMapper benefitMapper = new BenefitMapper();

    private BenefitServiceImpl benefitService;

    private UUID policyId;
    private BenefitRequest request;

    @BeforeEach
    void setUp() {
        benefitService = new BenefitServiceImpl(benefitRepository, benefitMapper, policyService);
        policyId = UUID.randomUUID();
        request = new BenefitRequest(policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES, new BigDecimal("100000.00"));
    }

    @Test
    void createSavesAllWhenTypesUniqueForPolicy() {
        BenefitRequest second = new BenefitRequest(
                policyId, BenefitType.PRESCRIPTION_MEDICINES, new BigDecimal("500.00"));
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(benefitRepository.existsByPolicyIdAndBenefitType(eq(policyId), any())).thenReturn(false);
        when(benefitRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<BenefitResponse> responses = benefitService.create(List.of(request, second));

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(BenefitResponse::benefitType)
                .containsExactly(BenefitType.EMERGENCY_MEDICAL_EXPENSES, BenefitType.PRESCRIPTION_MEDICINES);
        assertThat(responses).allSatisfy(response -> assertThat(response.policyId()).isEqualTo(policyId));
        verify(benefitRepository).saveAll(anyList());
    }

    @Test
    void createRejectsDuplicateTypeForPolicy() {
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(benefitRepository.existsByPolicyIdAndBenefitType(policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES))
                .thenReturn(true);

        assertThatThrownBy(() -> benefitService.create(List.of(request)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMERGENCY_MEDICAL_EXPENSES");
        verify(benefitRepository, never()).saveAll(anyList());
    }

    @Test
    void createRejectsDuplicateTypesWithinBatch() {
        BenefitRequest duplicate = new BenefitRequest(
                policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES, new BigDecimal("200000.00"));
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(benefitRepository.existsByPolicyIdAndBenefitType(policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES))
                .thenReturn(false);

        assertThatThrownBy(() -> benefitService.create(List.of(request, duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMERGENCY_MEDICAL_EXPENSES");
        verify(benefitRepository, never()).saveAll(anyList());
    }

    @Test
    void createRejectsLimitBelowMandatedMinimum() {
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        BenefitRequest belowMinimum = new BenefitRequest(
                policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES, new BigDecimal("100.00"));

        assertThatThrownBy(() -> benefitService.create(List.of(belowMinimum)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMERGENCY_MEDICAL_EXPENSES");
        verify(benefitRepository, never()).saveAll(anyList());
    }

    @Test
    void updateRejectsTypeAlreadyUsedByAnotherBenefit() {
        UUID id = UUID.randomUUID();
        Benefit existing = benefitMapper.toEntity(
                new BenefitRequest(policyId, BenefitType.PRESCRIPTION_MEDICINES, new BigDecimal("500.00")));
        when(benefitRepository.findById(id)).thenReturn(Optional.of(existing));
        when(benefitRepository.existsByPolicyIdAndBenefitTypeAndIdNot(
                policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES, id))
                .thenReturn(true);

        assertThatThrownBy(() -> benefitService.update(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMERGENCY_MEDICAL_EXPENSES");
        verify(benefitRepository, never()).save(any());
    }

    @Test
    void updateAllowsKeepingOwnType() {
        UUID id = UUID.randomUUID();
        Benefit existing = benefitMapper.toEntity(request);
        when(benefitRepository.findById(id)).thenReturn(Optional.of(existing));
        when(benefitRepository.existsByPolicyIdAndBenefitTypeAndIdNot(
                policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES, id))
                .thenReturn(false);
        when(benefitRepository.save(any(Benefit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenefitRequest update = new BenefitRequest(
                policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES, new BigDecimal("150000.00"));
        BenefitResponse response = benefitService.update(id, update);

        assertThat(response.benefitType()).isEqualTo(BenefitType.EMERGENCY_MEDICAL_EXPENSES);
        assertThat(response.limitAmount()).isEqualByComparingTo("150000.00");
    }

    @Test
    void updateRejectsLimitBelowMandatedMinimum() {
        UUID id = UUID.randomUUID();
        Benefit existing = benefitMapper.toEntity(request);
        when(benefitRepository.findById(id)).thenReturn(Optional.of(existing));

        BenefitRequest belowMinimum = new BenefitRequest(
                policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES, new BigDecimal("1.00"));

        assertThatThrownBy(() -> benefitService.update(id, belowMinimum))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMERGENCY_MEDICAL_EXPENSES");
        verify(benefitRepository, never()).save(any());
    }
}
