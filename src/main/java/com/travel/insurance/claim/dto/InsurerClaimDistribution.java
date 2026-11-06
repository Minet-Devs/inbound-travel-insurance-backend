package com.travel.insurance.claim.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code insurerId} is null for the "Unknown" bucket of claims with no insurer. */
public record InsurerClaimDistribution(UUID insurerId, String insurerName, long claimCount,
                                       BigDecimal approvedAmountUsd) {
}
