package com.travel.insurance.visitor;

import java.util.UUID;

/**
 * Event published when a visitor is deleted (soft-deleted).
 * Used to restore policy tokens to backing insurers.
 */
public record VisitorDeletedEvent(UUID visitorId, UUID policyId) {
}
