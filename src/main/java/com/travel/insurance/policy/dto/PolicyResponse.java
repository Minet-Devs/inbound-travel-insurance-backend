package com.travel.insurance.policy.dto;

import com.travel.insurance.policy.PolicyStatus;

import java.time.Instant;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        UUID insurerId,
        PolicyStatus status,
        Instant createdDate,
        Instant updatedDate
) {
}
