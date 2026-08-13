package com.travel.insurance.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceRequest(
        @NotNull UUID claimId,
        UUID medicalServiceId,
        @Size(max = 100) String invoiceNumber,
        LocalDate issueDate,
        @Size(max = 10) String currency,
        @NotNull @Positive BigDecimal totalAmount,
        @Valid List<InvoiceItemRequest> invoiceItems
) {
}
