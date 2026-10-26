package com.travel.insurance.policy;

import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerRepository;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decrements the policy token for a policy's backing insurer when a visitor is created
 * using that policy. This enforces the policy quota system where insurers have a limited
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
    private final OrganizationService organizationService;

    /**
     * Listens for visitor creation events and decrements the policy token
     * for the backing insurer of the policy the visitor was assigned to.
     *
     * @param event the visitor creation event containing visitor ID and policy ID
     */
    @EventListener
    @Transactional
    public void onVisitorCreated(VisitorCreatedEvent event) {
        Policy policy = policyRepository.findById(event.policyId())
                .orElseThrow(() -> new IllegalStateException(
                        "Policy not found: " + event.policyId()));

        Insurer insurer = insurerRepository.findById(policy.getInsurerId())
                .orElseThrow(() -> new IllegalStateException(
                        "Insurer not found: " + policy.getInsurerId()));

        // Single atomic UPDATE (not read-modify-write on the entity) so concurrent creates for the
        // same insurer neither lose a decrement nor hold the row lock across a long read/flush gap.
        if (insurerRepository.consumeOnePolicyToken(insurer.getId()) == 1) {
            long newToken = insurerRepository.findPolicyTokenById(insurer.getId()).orElseThrow();
            syncOrganization(insurer, newToken);
            log.info("Policy consumed for insurer: {}. Remaining tokens: {}",
                    insurer.getName(), newToken);
        } else {
            log.warn("Insurer {} has no available policies left", insurer.getName());
        }
    }

    private void syncOrganization(Insurer insurer, long token) {
        if (insurer.getOrganizationId() != null) {
            organizationService.syncPolicyToken(insurer.getOrganizationId(), token);
        }
    }
}
