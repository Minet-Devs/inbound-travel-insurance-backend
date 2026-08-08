package com.travel.insurance.policy;

/**
 * The three cover periods mandated by the Ministry of Health's Mandatory
 * Inbound Travel Health Insurance framework (min/max are inclusive day counts
 * between coverStartDate and coverEndDate).
 */
public enum PolicyType {
    SINGLE_ENTRY_UP_TO_30_DAYS(1, 30),
    SINGLE_ENTRY_31_TO_60_DAYS(31, 60),
    IPMI_61_DAYS_TO_12_MONTHS(61, 365);

    private final int minDays;
    private final int maxDays;

    PolicyType(int minDays, int maxDays) {
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public boolean isValidDuration(long days) {
        return days >= minDays && days <= maxDays;
    }

    public int getMinDays() {
        return minDays;
    }

    public int getMaxDays() {
        return maxDays;
    }
}
