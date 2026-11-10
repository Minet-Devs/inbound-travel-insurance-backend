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
        String invoiceNumber,
        String invoiceDate,
        BigDecimal invoiceAmount,
        String service,
        String department,
        BigDecimal claimAmountUsd,
        BigDecimal exchangeRate,
        String inboundClaimReference,
        List<Invoice> invoices) {

    /** One invoice of the claim. The flat invoice fields above remain the combined summary. */
    public record Invoice(String invoiceNumber, String invoiceDate, BigDecimal invoiceAmount, String currency) {
    }
}
