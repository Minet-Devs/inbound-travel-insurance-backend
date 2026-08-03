package com.travel.insurance.claim.dto;

import com.travel.insurance.claim.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ClaimDecisionRequest(
        @NotNull ClaimStatus status,
        @Positive BigDecimal approvedAmount,
        @Size(max = 1000) String reason
) {
}
