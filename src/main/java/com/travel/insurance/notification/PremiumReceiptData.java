package com.travel.insurance.notification;

import java.math.BigDecimal;

/**
 * Plain data holder assembled by {@link VisitorActivatedNotificationListener}
 * for {@link PolicyDocumentRenderer}. Internal to this package — not a DTO,
 * never crosses the web boundary. {@code pcfLevy}, {@code insurancePremiumLevy}
 * and {@code trainingLevy} are fractions (0-1), matching
 * {@code premiumreceipt.PremiumReceipt}; {@code totalPremium} and
 * {@code stampDuty} are flat KES amounts.
 */
record PremiumReceiptData(
        String visitorFullName,
        String passportNumber,
        String visitorAddress,
        String insurerName,
        String insurerLogoUrl,
        String insurerAddress,
        BigDecimal totalPremium,
        BigDecimal pcfLevy,
        BigDecimal insurancePremiumLevy,
        BigDecimal stampDuty,
        BigDecimal trainingLevy
) {
}
