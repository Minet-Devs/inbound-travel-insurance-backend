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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        request = new BenefitRequest(policyId, "Emergency Medical", new BigDecimal("100000.00"));
    }

    @Test
    void createSavesWhenNameUniqueForPolicy() {
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(benefitRepository.existsByPolicyIdAndNameIgnoreCase(policyId, "Emergency Medical"))
                .thenReturn(false);
        when(benefitRepository.save(any(Benefit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenefitResponse response = benefitService.create(request);

        assertThat(response.name()).isEqualTo("Emergency Medical");
        assertThat(response.policyId()).isEqualTo(policyId);
        verify(benefitRepository).save(any(Benefit.class));
    }

    @Test
    void createRejectsDuplicateNameForPolicy() {
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(benefitRepository.existsByPolicyIdAndNameIgnoreCase(policyId, "Emergency Medical"))
                .thenReturn(true);

        assertThatThrownBy(() -> benefitService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Emergency Medical");
        verify(benefitRepository, never()).save(any());
    }

    @Test
    void updateRejectsNameAlreadyUsedByAnotherBenefit() {
        UUID id = UUID.randomUUID();
        Benefit existing = benefitMapper.toEntity(
                new BenefitRequest(policyId, "Baggage Loss", new BigDecimal("50000.00")));
        when(benefitRepository.findById(id)).thenReturn(Optional.of(existing));
        when(benefitRepository.existsByPolicyIdAndNameIgnoreCaseAndIdNot(policyId, "Emergency Medical", id))
                .thenReturn(true);

        assertThatThrownBy(() -> benefitService.update(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Emergency Medical");
        verify(benefitRepository, never()).save(any());
    }

    @Test
    void updateAllowsKeepingOwnName() {
        UUID id = UUID.randomUUID();
        Benefit existing = benefitMapper.toEntity(request);
        when(benefitRepository.findById(id)).thenReturn(Optional.of(existing));
        when(benefitRepository.existsByPolicyIdAndNameIgnoreCaseAndIdNot(policyId, "Emergency Medical", id))
                .thenReturn(false);
        when(benefitRepository.save(any(Benefit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenefitRequest update = new BenefitRequest(policyId, "Emergency Medical", new BigDecimal("150000.00"));
        BenefitResponse response = benefitService.update(id, update);

        assertThat(response.name()).isEqualTo("Emergency Medical");
        assertThat(response.limitAmount()).isEqualByComparingTo("150000.00");
    }
}
