package com.travel.insurance.claim.dto;

import java.math.BigDecimal;

public record ClaimDashboardSummary(long totalClaims, BigDecimal totalApprovedAmountUsd) {
}
