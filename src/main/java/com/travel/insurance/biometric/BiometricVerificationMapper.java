package com.travel.insurance.biometric;

import com.travel.insurance.biometric.dto.BiometricVerificationRequest;
import com.travel.insurance.biometric.dto.BiometricVerificationResponse;
import org.springframework.stereotype.Component;

@Component
public class BiometricVerificationMapper {

    public BiometricVerification toEntity(BiometricVerificationRequest request) {
        BiometricVerification verification = new BiometricVerification();
        verification.setSubjectIdNumber(request.subjectIdNumber());
        verification.setSubjectIdType(request.subjectIdType());
        verification.setPolicyNumber(request.policyNumber());
        verification.setWorkstationId(request.workstationId());
        return verification;
    }

    public BiometricVerificationResponse toResponse(BiometricVerification verification) {
        return new BiometricVerificationResponse(
                verification.getId(),
                verification.getSubjectIdNumber(),
                verification.getSubjectIdType(),
                verification.getPolicyNumber(),
                verification.getWorkstationId(),
                verification.getEkycRequestId(),
                verification.getEmbededToken(),
                verification.getEmbededExpiry(),
                verification.getRequestUrl(),
                verification.getStatus(),
                verification.getResult(),
                verification.getRemainingAttempts(),
                verification.getCreatedDate(),
                verification.getUpdatedDate()
        );
    }
}
