package com.travel.insurance.preauthorization.dto;

import com.travel.insurance.common.domain.VisitType;
import com.travel.insurance.preauthorization.PreauthorizationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Body of {@code PUT /api/v1/preauthorizations/{id}}. The first block is required (as on create); every
 * field below it is optional and left unchanged when omitted.
 */
public record PreauthorizationUpdateRequest(
        @NotNull UUID policyId,
        @NotNull UUID visitorId,
        UUID icd11CodeId,
        @NotNull UUID benefitId,
        @NotNull UUID serviceProviderId,
        UUID medicalServiceId,
        @NotNull @Positive BigDecimal requestedAmount,
        @Size(max = 1000) String serviceDescription,
        @Valid List<PreauthorizationItemRequest> preauthorizationItems,
        VisitType visitType,
        @PositiveOrZero BigDecimal approvedAmount,
        @PositiveOrZero BigDecimal exchangeRate,
        @PositiveOrZero BigDecimal approvedAmountInDollars,
        PreauthorizationStatus status,
        @Size(max = 1000) String decisionReason,
        UUID decidedBy,
        Instant decidedAt,
        Boolean convertedToClaim,
        LocalDate treatmentDate,
        LocalDate dischargeDate
) {
}
