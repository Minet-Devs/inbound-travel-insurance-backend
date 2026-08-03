package com.travel.insurance.visitorbenefit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record VisitorBenefitRequest(
        @NotNull UUID visitorId,
        @NotNull UUID benefitId,
        @PositiveOrZero BigDecimal limitAmount
) {
}
