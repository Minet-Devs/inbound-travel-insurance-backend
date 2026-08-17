package com.travel.insurance.report.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProviderClaimReportSummary(
        int totalClaims,
        BigDecimal totalClaimedAmount,
        BigDecimal totalApprovedAmount,
        Map<String, Integer> byStatus
) {
}
