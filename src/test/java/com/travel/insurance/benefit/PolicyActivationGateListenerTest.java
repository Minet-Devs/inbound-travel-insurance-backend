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

    private List<BenefitResponse> fullCatalogWithMinimums() {
        Map<BenefitType, String> minimumsPlusOne = new EnumMap<>(BenefitType.class);
        minimumsPlusOne.put(BenefitType.PERSONAL_ACCIDENT, "20000.00");
        minimumsPlusOne.put(BenefitType.EMERGENCY_MEDICAL_EXPENSES, "20000.00");
        minimumsPlusOne.put(BenefitType.EMERGENCY_MEDICAL_EVACUATION, "25000.00");
        minimumsPlusOne.put(BenefitType.REPATRIATION_OF_MORTAL_REMAINS, "5000.00");
        minimumsPlusOne.put(BenefitType.HOSPITAL_BENEFITS, "1000.00");
        minimumsPlusOne.put(BenefitType.PRESCRIPTION_MEDICINES, "300.00");
        return minimumsPlusOne.entrySet().stream()
                .map(e -> benefit(e.getKey(), e.getValue()))
                .toList();
    }

    @Test
    void allowsActivationWhenFullCatalogMeetsCumulativeMinimum() {
        when(benefitService.listAllByPolicy(policyId)).thenReturn(fullCatalogWithMinimums());

        assertThatCode(() -> listener.onPolicyActivating(new PolicyActivatingEvent(policyId)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsActivationWhenABenefitTypeIsMissing() {
        List<BenefitResponse> missingPrescription = fullCatalogWithMinimums().stream()
                .filter(b -> b.benefitType() != BenefitType.PRESCRIPTION_MEDICINES)
                .toList();
        when(benefitService.listAllByPolicy(policyId)).thenReturn(missingPrescription);

        assertThatThrownBy(() -> listener.onPolicyActivating(new PolicyActivatingEvent(policyId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRESCRIPTION_MEDICINES");
    }

    @Test
    void rejectsActivationWhenCumulativeLimitBelowMandatedMinimum() {
        // All six types present (passes the coverage check) but each carries a
        // token limit, so the cumulative sum stays well under the USD 50,000 floor.
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
