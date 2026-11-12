package com.travel.insurance.claim;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    PARTIALLY_APPROVED,
    REJECTED,
    PAID,
    OPEN,
    AWAITING_MORE_INFORMATION,
    AWAITING_PAYMENT_APPROVAL,
    AWAITING_CLIENT_TO_PAY,
    PARTIALLY_PAID,
    CLOSED,
    INVALID,
    FINANCE_REJECTED,
    TIME_BARRED;

    /**
     * Statuses in which the claim's clinical details, invoices and documents may still be changed.
     * Once review starts (or a decision is made) the claim is locked.
     */
    public boolean isEditable() {
        return this == OPEN || this == AWAITING_MORE_INFORMATION || this == SUBMITTED;
    }

    /**
     * Maps a status string sent by Minet (e.g. {@code Awaiting_More_Information}, {@code FinanceRejected})
     * to a status, ignoring case, underscores and spaces. Empty when Minet sends a status we do not model.
     */
    public static Optional<ClaimStatus> fromMinet(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String key = normalise(value);
        return Arrays.stream(values()).filter(s -> normalise(s.name()).equals(key)).findFirst();
    }

    private static String normalise(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
    }
}
