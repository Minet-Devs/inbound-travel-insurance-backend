package com.travel.insurance.claim.dto;

import com.travel.insurance.claim.ClaimStatus;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.insurer.dto.InsurerResponse;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import com.travel.insurance.visitor.dto.VisitorResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        UUID policyId,
        UUID benefitId,
        UUID serviceProviderId,
        UUID preauthorizationId,
        UUID visitorId,
        VisitorResponse visitor,
        UUID insurerId,
        InsurerResponse insurer,
        BigDecimal claimedAmount,
        String currency,
        BigDecimal claimedAmountBase,
        BigDecimal exchangeRate,
        String baseCurrency,
        LocalDateTime fxRateDate,
        BigDecimal approvedAmount,
        String description,
        String prescription,
        List<Icd11CodeResponse> diagnoses,
        List<ProcedureResponse> procedures,
        List<InvoiceResponse> invoices,
        Set<UUID> documentIds,
        String decisionReason,
        ClaimStatus status,
        Instant createdDate,
        Instant updatedDate
) {
}
