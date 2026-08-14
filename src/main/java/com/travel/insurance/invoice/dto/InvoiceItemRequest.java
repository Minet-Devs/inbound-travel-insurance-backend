package com.travel.insurance.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceItemRequest(
        UUID medicalServiceId,
        @NotBlank @Size(max = 1000) String description,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal unitPrice,
        @NotNull @Positive BigDecimal amount,
        LocalDate serviceDate
) {
}
