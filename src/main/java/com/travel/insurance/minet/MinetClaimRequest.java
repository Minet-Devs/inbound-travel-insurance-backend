package com.travel.insurance.minet;

import java.math.BigDecimal;
import java.util.List;

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
        String service,
        String department,
        BigDecimal claimAmountUsd,
        BigDecimal exchangeRate,
        String inboundClaimReference,
        List<Invoice> invoices) {

    /** One invoice of the claim. Sent as an array even when the claim has a single invoice. */
    public record Invoice(String invoiceNumber, String invoiceDate, BigDecimal invoiceAmount, String currency) {
    }
}
