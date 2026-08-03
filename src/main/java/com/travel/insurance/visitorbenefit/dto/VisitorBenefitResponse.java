package com.travel.insurance.visitorbenefit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VisitorBenefitResponse(
        UUID id,
        UUID visitorId,
        UUID benefitId,
        String benefitName,
        BigDecimal limitAmount,
        Instant createdDate,
        Instant updatedDate
) {
}
