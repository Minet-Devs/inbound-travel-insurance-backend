package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.policy.PolicyActivatingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyActivationGateListenerTest {

    @Mock
    private BenefitService benefitService;

    @InjectMocks
    private PolicyActivationGateListener listener;

    private final UUID policyId = UUID.randomUUID();

    private BenefitResponse benefit(BenefitType type, String limit) {
        return new BenefitResponse(UUID.randomUUID(), policyId, type, new BigDecimal(limit),
                Instant.now(), Instant.now());
    }

    private List<BenefitResponse> fullCatalogWithFixedLimits() {
        Map<BenefitType, String> limits = new EnumMap<>(BenefitType.class);
        limits.put(BenefitType.MEDICAL_EXPENSES, "20000.00");
        limits.put(BenefitType.EMERGENCY_MEDICAL_EVACUATION, "25000.00");
        limits.put(BenefitType.PRESCRIBED_MEDICINES, "300.00");
        limits.put(BenefitType.MENTAL_ILLNESS, "1000.00");
        limits.put(BenefitType.REPATRIATION_OF_MORTAL_REMAINS, "5000.00");
        return limits.entrySet().stream()
                .map(e -> benefit(e.getKey(), e.getValue()))
                .toList();
    }

    @Test
    void allowsActivationWhenFullCatalogMeetsCumulativeMinimum() {
        when(benefitService.listAllByPolicy(policyId)).thenReturn(fullCatalogWithFixedLimits());

        assertThatCode(() -> listener.onPolicyActivating(new PolicyActivatingEvent(policyId)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsActivationWhenABenefitTypeIsMissing() {
        List<BenefitResponse> missingPrescription = fullCatalogWithFixedLimits().stream()
                .filter(b -> b.benefitType() != BenefitType.PRESCRIBED_MEDICINES)
                .toList();
        when(benefitService.listAllByPolicy(policyId)).thenReturn(missingPrescription);

        assertThatThrownBy(() -> listener.onPolicyActivating(new PolicyActivatingEvent(policyId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRESCRIBED_MEDICINES");
    }

    @Test
    void rejectsActivationWhenCumulativeLimitBelowMandatedMinimum() {
        // All types present (passes the coverage check) but each carries a token
        // limit, so the cumulative sum stays well under the USD 51,300 floor.
        List<BenefitResponse> belowCumulativeMinimum = Arrays.stream(BenefitType.values())
                .map(type -> benefit(type, "1.00"))
                .toList();
        when(benefitService.listAllByPolicy(policyId)).thenReturn(belowCumulativeMinimum);

        assertThatThrownBy(() -> listener.onPolicyActivating(new PolicyActivatingEvent(policyId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cumulative benefit limit");
    }

    @Test
    void rejectsActivationWhenNoBenefitsExist() {
        when(benefitService.listAllByPolicy(policyId)).thenReturn(List.of());

        assertThatThrownBy(() -> listener.onPolicyActivating(new PolicyActivatingEvent(policyId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing mandated benefit type");
    }
}
