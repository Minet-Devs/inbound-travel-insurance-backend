package com.travel.insurance.preauthorization.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PreauthorizationRequest(
        @NotNull UUID policyId,
        @NotNull UUID benefitId,
        @NotNull UUID serviceProviderId,
        @NotNull @Positive BigDecimal requestedAmount,
        @Size(max = 1000) String serviceDescription
) {
}
