package com.travel.insurance.benefit.dto;

import com.travel.insurance.benefit.BenefitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record BenefitRequest(
        @NotNull UUID policyId,
        @NotNull BenefitType benefitType,
        @NotNull @PositiveOrZero BigDecimal limitAmount
) {
}
