package com.travel.insurance.visitor;

import java.util.UUID;

public record VisitorCreatedEvent(UUID visitorId, UUID policyId) {
}
