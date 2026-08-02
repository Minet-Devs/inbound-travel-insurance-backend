package com.travel.insurance.policy.dto;

import com.travel.insurance.policy.PolicyStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String policyNumber,
        Set<UUID> insurerIds,
        LocalDate coverStartDate,
        LocalDate coverEndDate,
        PolicyStatus status,
        Instant createdDate,
        Instant updatedDate
) {
}
