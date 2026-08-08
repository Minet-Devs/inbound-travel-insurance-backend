package com.travel.insurance.benefit.dto;

import com.travel.insurance.benefit.BenefitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BenefitResponse(
        UUID id,
        UUID policyId,
        BenefitType benefitType,
        BigDecimal limitAmount,
        Instant createdDate,
        Instant updatedDate
) {
}
