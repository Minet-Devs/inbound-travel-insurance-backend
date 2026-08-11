package com.travel.insurance.benefit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BenefitResponse(
        UUID id,
        String benefitName,
        BigDecimal limitAmount,
        Instant createdDate,
        Instant updatedDate
) {
}
