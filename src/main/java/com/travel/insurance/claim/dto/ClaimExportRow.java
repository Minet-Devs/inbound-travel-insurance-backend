package com.travel.insurance.claim.dto;

import com.travel.insurance.common.domain.VisitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One row of the claims Excel export: one per invoice item (or one per claim when it has no invoice items). */
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
        LocalDate invoiceDate,
        BigDecimal invoiceAmount,
        String serviceName,
        String departmentName,
        BigDecimal claimedAmountBase,
        BigDecimal exchangeRate
) {
}
