package com.travel.insurance.preauthorization.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Approved and partially approved pre-authorizations of one insurer; {@code approvedAmount} is the sum of approvedAmount. */
public record InsurerPreauthorizationDistribution(UUID insurerId, String insurerName,
                                                  long preauthorizationCount, BigDecimal approvedAmount) {
}
