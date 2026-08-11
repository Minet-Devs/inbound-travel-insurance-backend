package com.travel.insurance.benefit;

import java.math.BigDecimal;

/**
 * Fixed catalog of insured events and their limits of cover taken directly from
 * the Inbound Travel Medical Insurance Policy Document (July 2026), §5 "Limits
 * of Cover". Every policy inherits this exact list on creation
 * ({@link BenefitService#provisionFixedBenefits(java.util.UUID)}); the limits
 * are fixed, not configurable. A policy's benefits must cover every type here
 * with a combined limit of at least {@link #MANDATED_CUMULATIVE_MINIMUM} before
 * it can go ACTIVE.
 */
public enum BenefitType {
    MEDICAL_EXPENSES(new BigDecimal("20000.00")),
    EMERGENCY_MEDICAL_EVACUATION(new BigDecimal("25000.00")),
    PRESCRIBED_MEDICINES(new BigDecimal("300.00")),
    MENTAL_ILLNESS(new BigDecimal("1000.00")),
    REPATRIATION_OF_MORTAL_REMAINS(new BigDecimal("5000.00"));

    /**
     * Sum of every type's fixed limit (USD 51,300) — the cumulative cover a
     * fully provisioned policy carries.
     */
    public static final BigDecimal MANDATED_CUMULATIVE_MINIMUM = new BigDecimal("51300.00");

    private final BigDecimal fixedLimit;

    BenefitType(BigDecimal fixedLimit) {
        this.fixedLimit = fixedLimit;
    }

    public BigDecimal getFixedLimit() {
        return fixedLimit;
    }
}
