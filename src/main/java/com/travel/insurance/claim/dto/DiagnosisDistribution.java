package com.travel.insurance.claim.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code percentage} (0-100, 2 dp) is this diagnosis's share of all diagnosis occurrences in the same scope. */
public record DiagnosisDistribution(UUID diagnosisId, String code, String title, long claimCount,
                                    BigDecimal percentage) {
}
