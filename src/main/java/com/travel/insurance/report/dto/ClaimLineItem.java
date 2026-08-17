package com.travel.insurance.report.dto;

import java.math.BigDecimal;

public record ClaimLineItem(
        String serviceName,
        String departmentName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {
}
