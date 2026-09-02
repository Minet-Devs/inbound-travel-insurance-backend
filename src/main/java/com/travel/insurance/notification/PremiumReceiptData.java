package com.travel.insurance.notification;

import java.math.BigDecimal;

/**
 * Plain data holder assembled by {@link VisitorActivatedNotificationListener}
 * for {@link PolicyDocumentRenderer}. Internal to this package — not a DTO,
 * never crosses the web boundary.
 */
record PremiumReceiptData(
        String visitorFullName,
        String passportNumber,
        String certificateSerialNumber,
        String visitorAddress,
        String visitorNationality,
        String insurerName,
        String insurerLogoUrl,
        String insurerAddress,
        BigDecimal totalPremium
) {
}
