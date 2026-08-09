package com.travel.insurance.benefit.dto;

import com.travel.insurance.benefit.BenefitType;

import java.math.BigDecimal;

public record BenefitTypeResponse(
        BenefitType benefitType,
        BigDecimal minimumLimit
) {
}
