package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.benefit.dto.BenefitTypeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitServiceImplTest {

    @Mock
    private BenefitRepository benefitRepository;

    private final BenefitMapper benefitMapper = new BenefitMapper();

    private BenefitServiceImpl benefitService;

    private UUID policyId;

    @BeforeEach
    void setUp() {
        benefitService = new BenefitServiceImpl(benefitRepository, benefitMapper);
        policyId = UUID.randomUUID();
    }

    @Test
    void listBenefitTypesReturnsFixedCatalogWithFixedLimits() {
        List<BenefitTypeResponse> types = benefitService.listBenefitTypes();

        assertThat(types).hasSize(BenefitType.values().length);
        assertThat(types).extracting(BenefitTypeResponse::benefitType)
                .containsExactlyInAnyOrder(BenefitType.values());
        assertThat(types).filteredOn(t -> t.benefitType() == BenefitType.MEDICAL_EXPENSES)
                .extracting(BenefitTypeResponse::fixedLimit)
                .containsExactly(new BigDecimal("20000.00"));
    }

    @Test
    void provisionFixedBenefitsCreatesEveryTypeWithItsFixedLimit() {
        when(benefitRepository.existsByPolicyIdAndBenefitType(eq(policyId), any())).thenReturn(false);
        when(benefitRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<BenefitResponse> responses = benefitService.provisionFixedBenefits(policyId);

        assertThat(responses).extracting(BenefitResponse::benefitType)
                .containsExactlyInAnyOrder(BenefitType.values());
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.policyId()).isEqualTo(policyId);
            assertThat(response.limitAmount())
                    .isEqualByComparingTo(response.benefitType().getFixedLimit());
        });
    }

    @Test
    void provisionFixedBenefitsSkipsTypesAlreadyPresent() {
        when(benefitRepository.existsByPolicyIdAndBenefitType(eq(policyId), any())).thenReturn(false);
        when(benefitRepository.existsByPolicyIdAndBenefitType(policyId, BenefitType.MEDICAL_EXPENSES))
                .thenReturn(true);
        when(benefitRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<BenefitResponse> responses = benefitService.provisionFixedBenefits(policyId);

        assertThat(responses).extracting(BenefitResponse::benefitType)
                .doesNotContain(BenefitType.MEDICAL_EXPENSES)
                .hasSize(BenefitType.values().length - 1);
    }
}
