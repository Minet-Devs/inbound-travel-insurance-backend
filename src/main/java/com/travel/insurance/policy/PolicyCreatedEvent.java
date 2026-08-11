package com.travel.insurance.policy;

import java.util.UUID;

/**
 * Published after a policy is persisted so other features can react to a new
 * policy — notably the benefit feature, which provisions the fixed benefit
 * catalog onto it.
 */
public record PolicyCreatedEvent(UUID policyId) {
}
