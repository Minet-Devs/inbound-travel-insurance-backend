package com.travel.insurance.premiumreceipt.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * Every field is optional, and only the fields present (non-null) in the
 * request are applied — omitted fields are left untouched.
 */
public record PremiumReceiptPatchRequest(
        @DecimalMin("0") BigDecimal totalPremium,
        @DecimalMin("0") @DecimalMax("1") BigDecimal pcfLevy,
        @DecimalMin("0") @DecimalMax("1") BigDecimal insurancePremiumLevy,
        @DecimalMin("0") BigDecimal stampDuty,
        @DecimalMin("0") @DecimalMax("1") BigDecimal trainingLevy
) {
}
