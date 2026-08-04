package com.travel.insurance.biometric;

import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.biometric.dto.BiometricVerificationRequest;
import com.travel.insurance.biometric.dto.BiometricVerificationResponse;
import org.springframework.http.HttpStatusCode;

import java.util.UUID;

public interface BiometricVerificationService {

    BiometricVerificationResponse create(BiometricVerificationRequest request);

    BiometricVerificationResponse getById(UUID id);

    void handleCallback(BiometricCallbackPayload payload);

    HttpStatusCode resend(UUID id);

    BiometricVerification getEntityById(UUID id);
}
