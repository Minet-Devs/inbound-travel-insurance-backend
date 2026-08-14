package com.travel.insurance.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID claimId,
        UUID medicalServiceId,
        String medicalServiceName,
        String invoiceNumber,
        LocalDate issueDate,
        String currency,
        BigDecimal totalAmount,
        BigDecimal exchangeRate,
        String baseCurrency,
        BigDecimal baseTotalAmount,
        LocalDateTime fxRateDate,
        List<InvoiceItemResponse> invoiceItems,
        Instant createdDate,
        Instant updatedDate
) {
}
