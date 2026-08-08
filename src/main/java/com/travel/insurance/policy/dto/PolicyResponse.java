package com.travel.insurance.policy.dto;

import com.travel.insurance.policy.PolicyStatus;
import com.travel.insurance.policy.PolicyType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String policyNumber,
        Set<UUID> insurerIds,
        PolicyType policyType,
        LocalDate coverStartDate,
        LocalDate coverEndDate,
        PolicyStatus status,
        Instant createdDate,
        Instant updatedDate
) {
}
