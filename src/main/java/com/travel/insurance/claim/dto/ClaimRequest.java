package com.travel.insurance.claim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ClaimRequest(
        @NotNull UUID policyId,
        @NotNull UUID benefitId,
        UUID serviceProviderId,
        UUID preauthorizationId,
        UUID visitorId,
        @NotNull @Positive BigDecimal claimedAmount,
        @Size(max = 1000) String description,
        @Size(max = 2000) String prescription,
        Set<UUID> diagnosisIds,
        Set<UUID> procedureIds,
        Set<UUID> invoiceIds,
        Set<UUID> documentIds
) {
}
