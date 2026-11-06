package com.travel.insurance.preauthorization.dto;

public record PreauthorizationDashboardSummary(long totalPreauthorizations, long approved, long rejected,
                                               long partiallyApproved) {
}
