package com.travel.insurance.benefit;

import java.math.BigDecimal;

/**
 * Fixed catalog of insured events mandated by the Ministry of Health's
 * Mandatory Inbound Travel Health Insurance framework (§6.2/6.3), each with
 * its own mandated minimum limit. A policy's benefits must cover every type
 * here with a combined limit of at least {@link #MANDATED_CUMULATIVE_MINIMUM}
 * before it can go ACTIVE.
 */
public enum BenefitType {
    PERSONAL_ACCIDENT(BigDecimal.ZERO),
    EMERGENCY_MEDICAL_EXPENSES(new BigDecimal("20000.00")),
    EMERGENCY_MEDICAL_EVACUATION(new BigDecimal("25000.00")),
    REPATRIATION_OF_MORTAL_REMAINS(new BigDecimal("5000.00")),
    HOSPITAL_BENEFITS(new BigDecimal("1000.00")),
    PRESCRIPTION_MEDICINES(new BigDecimal("300.00"));

    public static final BigDecimal MANDATED_CUMULATIVE_MINIMUM = new BigDecimal("50000.00");

    private final BigDecimal minimumLimit;

    BenefitType(BigDecimal minimumLimit) {
        this.minimumLimit = minimumLimit;
    }

    public BigDecimal getMinimumLimit() {
        return minimumLimit;
    }
}
