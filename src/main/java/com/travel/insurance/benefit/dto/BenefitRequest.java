package com.travel.insurance.benefit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record BenefitRequest(
        @NotNull UUID policyId,
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal limitAmount
) {
}
