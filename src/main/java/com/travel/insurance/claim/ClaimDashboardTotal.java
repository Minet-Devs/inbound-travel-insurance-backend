package com.travel.insurance.claim;

import java.math.BigDecimal;
import java.util.UUID;

/** Claim count and approved USD total; {@code getGroupId()} is only populated by the grouped queries. */
public interface ClaimDashboardTotal {

    UUID getGroupId();

    long getClaimCount();

    BigDecimal getApprovedAmountUsd();
}
