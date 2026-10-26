package com.travel.insurance.claim.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ClaimExportRow(
        String visitorName,
        String passportNumber,
        String serviceProviderName,
        String benefitName,
        String insurerName,
        BigDecimal amount,
        Instant claimDate
) {
}
