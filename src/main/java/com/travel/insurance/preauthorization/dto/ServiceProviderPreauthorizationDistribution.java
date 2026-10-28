package com.travel.insurance.preauthorization.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Approved and partially approved pre-authorizations of one service provider; {@code approvedAmount} is the sum of approvedAmount. */
public record ServiceProviderPreauthorizationDistribution(UUID serviceProviderId, String serviceProviderName,
                                                          long preauthorizationCount, BigDecimal approvedAmount) {
}
