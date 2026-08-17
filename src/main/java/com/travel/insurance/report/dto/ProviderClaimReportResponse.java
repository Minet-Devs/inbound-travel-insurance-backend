package com.travel.insurance.report.dto;

import java.util.List;

public record ProviderClaimReportResponse(
        ProviderClaimReportSummary summary,
        List<ProviderClaimReportRow> claims,
        int page,
        int size,
        long totalElements
) {
}
