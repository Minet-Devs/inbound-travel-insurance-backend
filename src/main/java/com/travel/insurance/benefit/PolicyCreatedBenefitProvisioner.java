package com.travel.insurance.benefit;

import com.travel.insurance.policy.PolicyCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions the fixed {@link BenefitType} catalog onto every newly created
 * policy, so each policy inherits the mandated benefit list with its fixed
 * limits (Policy Document July 2026, §5). Listens for {@link PolicyCreatedEvent}
 * rather than depending on PolicyService directly, and runs in the creating
 * transaction so the benefits exist before the activation gate checks them.
 */
@Component
@RequiredArgsConstructor
public class PolicyCreatedBenefitProvisioner {

    private final BenefitService benefitService;

    @EventListener
    @Transactional
    public void onPolicyCreated(PolicyCreatedEvent event) {
        benefitService.provisionFixedBenefits(event.policyId());
    }
}
