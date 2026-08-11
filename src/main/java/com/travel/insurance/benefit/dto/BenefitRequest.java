package com.travel.insurance.benefit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record BenefitRequest(
        @NotBlank String benefitName,
        @NotNull @PositiveOrZero BigDecimal limitAmount
) {
}
