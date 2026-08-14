package com.travel.insurance.claim;

import java.math.BigDecimal;
import java.util.UUID;

public record ClaimUtilizationTotal(UUID visitorId, UUID benefitId, BigDecimal totalClaimed) {
}