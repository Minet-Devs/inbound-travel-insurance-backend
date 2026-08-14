package com.travel.insurance.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        UUID medicalServiceId,
        String medicalServiceName,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        LocalDate serviceDate
) {
}
