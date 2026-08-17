package com.travel.insurance.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClaimInvoiceGroup(
        UUID invoiceId,
        String invoiceNumber,
        LocalDate issueDate,
        String currency,
        BigDecimal totalAmount,
        List<ClaimLineItem> items
) {
}
