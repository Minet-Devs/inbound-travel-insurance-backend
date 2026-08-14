package com.travel.insurance.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID claimId,
        String invoiceNumber,
        LocalDate issueDate,
        String currency,
        BigDecimal totalAmount,
        List<InvoiceItemResponse> invoiceItems,
        Instant createdDate,
        Instant updatedDate
) {
}
