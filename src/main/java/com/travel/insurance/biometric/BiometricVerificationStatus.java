package com.travel.insurance.biometric;

import java.util.Locale;

public enum BiometricVerificationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED;

    public static BiometricVerificationStatus from(String ekycStatus) {
        if (ekycStatus == null) {
            return REJECTED;
        }
        return switch (ekycStatus.toLowerCase(Locale.ROOT)) {
            case "accepted" -> ACCEPTED;
            case "expired" -> EXPIRED;
            default -> REJECTED;
        };
    }
}
