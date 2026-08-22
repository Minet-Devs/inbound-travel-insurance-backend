package com.travel.insurance.policy.dto;

import com.travel.insurance.policy.PolicyStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PolicyRequest(
        @NotNull UUID insurerId,
        PolicyStatus status
) {
}
