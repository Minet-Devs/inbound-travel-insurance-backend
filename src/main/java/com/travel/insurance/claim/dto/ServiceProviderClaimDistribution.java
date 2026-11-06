package com.travel.insurance.claim.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code serviceProviderId} is null for the "Unknown" bucket of claims with no service provider. */
public record ServiceProviderClaimDistribution(UUID serviceProviderId, String serviceProviderName,
                                               long claimCount, BigDecimal approvedAmountUsd) {
}
