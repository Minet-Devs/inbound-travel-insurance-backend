package com.travel.insurance.biometric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BiometricVerificationRequest(
        @NotBlank @Size(max = 100) String subjectIdNumber,
        @NotBlank @Size(max = 50) String subjectIdType,
        @NotBlank @Size(max = 100) String policyNumber,
        @NotBlank @Size(max = 255) String workstationId
) {
}
