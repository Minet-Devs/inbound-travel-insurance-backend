package com.travel.insurance.preauthorization.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PreauthorizationItemResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        LocalDate serviceDate
) {
}