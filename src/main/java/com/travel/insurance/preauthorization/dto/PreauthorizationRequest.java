package com.travel.insurance.preauthorization.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PreauthorizationRequest(
        @NotNull UUID policyId,
        @NotNull UUID visitorId,
        @NotNull UUID icd11CodeId,
        @NotNull UUID benefitId,
        @NotNull UUID serviceProviderId,
        UUID medicalServiceId,
        @NotNull @Positive BigDecimal requestedAmount,
        @Size(max = 1000) String serviceDescription,
        @Valid List<PreauthorizationItemRequest> preauthorizationItems
) {
}
