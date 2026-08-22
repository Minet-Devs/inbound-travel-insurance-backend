package com.travel.insurance.insurer;

import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.policy.dto.PolicyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions a starter Policy whenever an Insurer is created, so every
 * insurer has a policy to back visitors against without a separate manual
 * step. The policy is created with the default status (ACTIVE).
 */
@Component
@RequiredArgsConstructor
public class InsurerCreatedListener {

    private final PolicyService policyService;

    @EventListener
    @Transactional
    public void onInsurerCreated(InsurerCreatedEvent event) {
        policyService.create(new PolicyRequest(event.insurerId(), null));
    }
}
