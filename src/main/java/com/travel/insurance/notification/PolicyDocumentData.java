package com.travel.insurance.notification;

import com.travel.insurance.policy.PolicyType;
import com.travel.insurance.visitor.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Plain data holder assembled by {@link VisitorActivatedNotificationListener}
 * for {@link PolicyDocumentRenderer}. Internal to this package — not a DTO,
 * never crosses the web boundary. Deliberately excludes
 * {@code Visitor.underlyingConditions}: it isn't present on any of the
 * reference certificates this template is modeled on, and there's no reason
 * to widen PII exposure over email with it.
 */
record PolicyDocumentData(
        String visitorFullName,
        String passportNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String nationality,
        String address,
        String email,
        String phoneNumber,
        LocalDate dateIn,
        LocalDate dateOut,
        String reasonForTravel,
        String policyNumber,
        PolicyType policyType,
        List<String> insurerNames,
        String underwriterLogoUrl,
        String esignatureUrl,
        List<BenefitLine> benefits,
        String emergencyAssistancePhone,
        String emergencyAssistanceEmail
) {

    record BenefitLine(String benefitName, BigDecimal limitAmount) {
    }
}
