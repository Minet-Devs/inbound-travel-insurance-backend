package com.travel.insurance.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProviderClaimReportRow(
        UUID claimId,
        String claimNumber,
        String status,
        Instant createdDate,
        String visitorName,
        String benefitName,
        BigDecimal claimedAmount,
        BigDecimal approvedAmount
) {
}
