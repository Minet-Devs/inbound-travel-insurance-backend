package com.travel.insurance.minet;

import java.math.BigDecimal;

/** Payload for Minet's inbound claim API. Dates are {@code yyyy-MM-dd} strings. */
public record MinetClaimRequest(
        String providerName,
        String providerId,
        String benefit,
        String passportNumber,
        String claimDate,
        String visitType,
        String diagnosisCode,
        String diagnosisDescription,
        String visitDate,
        BigDecimal amountClaimed,
        String invoiceNumber,
        String invoiceDate,
        BigDecimal invoiceAmount,
        String service,
        String department,
        BigDecimal claimAmountUsd,
        BigDecimal exchangeRate,
        String inboundClaimReference) {
}
