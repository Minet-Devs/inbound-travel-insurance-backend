package com.travel.insurance.preauthorization.dto;

import com.travel.insurance.preauthorization.PreauthorizationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PreauthorizationResponse(
        UUID id,
        UUID policyId,
        UUID benefitId,
        UUID serviceProviderId,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        String serviceDescription,
        String decisionReason,
        PreauthorizationStatus status,
        Instant createdDate,
        Instant updatedDate
) {
}
