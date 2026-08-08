package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.policy.PolicyActivatingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforces the Ministry of Health framework's activation gate (§6.2/6.3): a
 * policy may only go ACTIVE once its benefits cover every {@link BenefitType}
 * with a combined limit of at least {@link BenefitType#MANDATED_CUMULATIVE_MINIMUM}.
 * Listens for {@link PolicyActivatingEvent} rather than depending on
 * PolicyService directly, since PolicyServiceImpl.create/update already runs
 * inside the transaction being validated — throwing here rolls it back.
 */
@Component
@RequiredArgsConstructor
public class PolicyActivationGateListener {

    private final BenefitService benefitService;

    @EventListener
    public void onPolicyActivating(PolicyActivatingEvent event) {
        List<BenefitResponse> benefits = benefitService.listAllByPolicy(event.policyId());

        Set<BenefitType> coveredTypes = benefits.stream()
                .map(BenefitResponse::benefitType)
                .collect(Collectors.toSet());
        Set<BenefitType> missingTypes = EnumSet.allOf(BenefitType.class);
        missingTypes.removeAll(coveredTypes);
        if (!missingTypes.isEmpty()) {
            throw new IllegalStateException(
                    "Policy cannot be activated: missing mandated benefit type(s) " + missingTypes);
        }

        BigDecimal cumulativeLimit = benefits.stream()
                .map(BenefitResponse::limitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (cumulativeLimit.compareTo(BenefitType.MANDATED_CUMULATIVE_MINIMUM) < 0) {
            throw new IllegalStateException(
                    "Policy cannot be activated: cumulative benefit limit %s is below the mandated minimum of %s"
                            .formatted(cumulativeLimit, BenefitType.MANDATED_CUMULATIVE_MINIMUM));
        }
    }
}
