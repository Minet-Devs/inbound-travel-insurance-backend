package com.travel.insurance.policy;

import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerRepository;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Decrements policy tokens for insurers when a visitor is created using their policy.
 * When a visitor is assigned to a policy, each backing insurer's available policy count
 * is decremented by 1. This enforces the policy quota system where insurers have a limited
 * number of policies they can issue.
 *
 * Example:
 * - Minet Insurance has 1000 policies (policyToken = 1000)
 * - A policy is created and linked to Minet
 * - A visitor is created using that policy
 * - Minet's policyToken becomes 999
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyConsumptionListener {

    private final PolicyRepository policyRepository;
    private final InsurerRepository insurerRepository;

    /**
     * Listens for visitor creation events and decrements the policy tokens
     * for all backing insurers of the policy the visitor was assigned to.
     *
     * @param event the visitor creation event containing visitor ID and policy ID
     */
    @EventListener
    @Transactional
    public void onVisitorCreated(VisitorCreatedEvent event) {
        Policy policy = policyRepository.findById(event.policyId())
                .orElseThrow(() -> new IllegalStateException(
                        "Policy not found: " + event.policyId()));

        // Decrement policy token for each backing insurer
        for (UUID insurerId : policy.getInsurerIds()) {
            Insurer insurer = insurerRepository.findById(insurerId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Insurer not found: " + insurerId));

            if (insurer.getPolicyToken() != null && insurer.getPolicyToken() > 0) {
                long newToken = insurer.getPolicyToken() - 1;
                insurer.setPolicyToken(newToken);
                insurerRepository.save(insurer);
                log.info("Policy consumed for insurer: {}. Remaining tokens: {}",
                        insurer.getName(), newToken);
            } else {
                log.warn("Insurer {} has no available policies left", insurer.getName());
            }
        }
    }
}
