package com.travel.insurance.preauthorization.dto;

import java.math.BigDecimal;

/** Pre-authorizations of visitors of one nationality (ISO alpha-3, upper-cased); amounts are in the stored (KES) currency. */
public record CountryPreauthorizationTotal(String country, long preauthorizationCount,
                                           BigDecimal requestedAmount, BigDecimal approvedAmount) {
}
