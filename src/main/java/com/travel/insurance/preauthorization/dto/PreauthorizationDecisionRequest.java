package com.travel.insurance.preauthorization.dto;

import com.travel.insurance.preauthorization.PreauthorizationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PreauthorizationDecisionRequest(
        @NotNull PreauthorizationStatus status,
        @Positive BigDecimal approvedAmount,
        @Size(max = 1000) String reason
) {
}
