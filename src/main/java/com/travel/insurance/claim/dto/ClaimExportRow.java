package com.travel.insurance.claim.dto;

import com.travel.insurance.claim.ClaimStatus;
import com.travel.insurance.common.domain.VisitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row of the claims Excel export: exactly one per claim. Invoice amount is the sum of the claim's invoices;
 * invoice number, service and department are the distinct values joined with "; "; visit date is the earliest item date.
 * The invoice date column in the sheet is the claim date.
 * The last column is the claim's current status (e.g. APPROVED, PARTIALLY_APPROVED, REJECTED, PAID).
 */
public record ClaimExportRow(
        String serviceProviderName,
        UUID serviceProviderId,
        String benefitName,
        String passportNumber,
        Instant claimDate,
        VisitType visitType,
        String diagnosisCodes,
        String diagnosisDescriptions,
        LocalDate visitDate,
        BigDecimal claimedAmount,
        String invoiceNumber,
        BigDecimal invoiceAmount,
        String serviceName,
        String departmentName,
        BigDecimal claimedAmountBase,
        BigDecimal exchangeRate,
        ClaimStatus status
) {
}
