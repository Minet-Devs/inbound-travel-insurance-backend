package com.travel.insurance.visitor;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum VisitorStatus {
    PENDING,
    ACTIVE,
    DEACTIVATED,
    SUSPENDED;

    // Lifecycle: PENDING -> ACTIVE, ACTIVE <-> SUSPENDED, and DEACTIVATED is terminal.
    private static final Map<VisitorStatus, Set<VisitorStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, EnumSet.of(ACTIVE),
            ACTIVE, EnumSet.of(SUSPENDED, DEACTIVATED),
            SUSPENDED, EnumSet.of(ACTIVE, DEACTIVATED),
            DEACTIVATED, EnumSet.noneOf(VisitorStatus.class));

    public boolean canTransitionTo(VisitorStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
