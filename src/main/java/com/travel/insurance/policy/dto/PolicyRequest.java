package com.travel.insurance.policy.dto;

import com.travel.insurance.policy.PolicyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record PolicyRequest(
        @NotBlank String policyNumber,
        @NotEmpty Set<UUID> insurerIds,
        @NotNull LocalDate coverStartDate,
        @NotNull LocalDate coverEndDate,
        PolicyStatus status
) {
}
