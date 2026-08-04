package com.travel.insurance.visitor;

import java.util.UUID;

public record VisitorStatusChangedEvent(UUID visitorId, VisitorStatus newStatus) {
}
