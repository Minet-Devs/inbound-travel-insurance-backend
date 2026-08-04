package com.travel.insurance.biometric.dto;

import com.travel.insurance.biometric.BiometricVerificationStatus;

import java.time.Instant;
import java.util.UUID;

public record BiometricVerificationResponse(
        UUID id,
        String subjectIdNumber,
        String subjectIdType,
        String policyNumber,
        String workstationId,
        String ekycRequestId,
        String embededToken,
        String embededExpiry,
        String requestUrl,
        BiometricVerificationStatus status,
        String result,
        Integer remainingAttempts,
        Instant createdDate,
        Instant updatedDate
) {
}
