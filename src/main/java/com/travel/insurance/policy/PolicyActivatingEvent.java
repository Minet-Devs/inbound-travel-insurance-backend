package com.travel.insurance.policy;

import java.util.UUID;

/**
 * Published synchronously, before commit, whenever a policy is about to
 * transition into {@link PolicyStatus#ACTIVE}. Listeners may throw to reject
 * the transition (e.g. the mandated benefit catalog/coverage gate in the
 * {@code benefit} package) — the exception propagates back through this
 * transactional call and rolls back the save.
 */
public record PolicyActivatingEvent(UUID policyId) {
}
