package com.travel.insurance.claim.dto;

import com.travel.insurance.claim.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        UUID policyId,
        UUID benefitId,
        UUID serviceProviderId,
        UUID preauthorizationId,
        BigDecimal claimedAmount,
        BigDecimal approvedAmount,
        String description,
        String decisionReason,
        ClaimStatus status,
        Instant createdDate,
        Instant updatedDate
) {
}
