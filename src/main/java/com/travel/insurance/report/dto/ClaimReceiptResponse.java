package com.travel.insurance.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClaimReceiptResponse(
        UUID claimId,
        String claimNumber,
        String status,
        Instant createdDate,
        String visitorName,
        String passportNumber,
        LocalDate dateOfBirth,
        String nationality,
        LocalDate dateIn,
        LocalDate dateOut,
        String benefitName,
        String providerName,
        List<ClaimInvoiceGroup> invoices,
        List<String> diagnoses,
        List<String> procedures,
        BigDecimal totalAmount
) {
}
