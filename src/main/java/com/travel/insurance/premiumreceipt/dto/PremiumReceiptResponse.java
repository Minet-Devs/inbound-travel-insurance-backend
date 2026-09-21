package com.travel.insurance.premiumreceipt.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PremiumReceiptResponse(
        UUID id,
        BigDecimal totalPremium,
        BigDecimal minorPremium,
        BigDecimal infantPremium,
        BigDecimal pcfLevy,
        BigDecimal insurancePremiumLevy,
        BigDecimal stampDuty,
        BigDecimal trainingLevy,
        Instant createdDate,
        Instant updatedDate
) {
}
