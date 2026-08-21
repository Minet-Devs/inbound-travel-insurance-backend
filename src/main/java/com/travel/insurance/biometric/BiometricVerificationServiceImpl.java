package com.travel.insurance.biometric;

import com.travel.insurance.biometric.client.EkYcClient;
import com.travel.insurance.biometric.client.EkYcCreateRequest;
import com.travel.insurance.biometric.client.EkYcEmbededResponse;
import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.biometric.dto.BiometricVerificationRequest;
import com.travel.insurance.biometric.dto.BiometricVerificationResponse;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.config.EkYcProperties;
import com.travel.insurance.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BiometricVerificationServiceImpl implements BiometricVerificationService {

    private static final String EKYC_REASON = "Medical Care";
    private static final String EKYC_AGENT_ID_NUMBER = "27927159";
    private static final String EKYC_AGENT_ID_TYPE = "citizen";
    private static final int EKYC_EXPIRES_IN_SECONDS = 3600;
    private static final String EKYC_SERVICE_ID = "default";
    private static final int EKYC_TOTAL_ATTEMPTS = 4;
    private static final String EKYC_REQUEST_MODE = "embeded";
    private static final int EKYC_POOR_QUALITY_RESULT_ATTEMPTS = 3;
    private static final String EKYC_LOCATION_NAME = "13110-Nairobi Hospital";
    private static final String EKYC_DEVICE_ID = "";
    private static final String EKYC_DEVICE_NAME = "";

    private final BiometricVerificationRepository repository;
    private final BiometricVerificationMapper mapper;
    private final EkYcClient ekycClient;
    private final EkYcProperties properties;
    private final EventPublisher eventPublisher;

    @Override
    public BiometricVerificationResponse create(BiometricVerificationRequest request) {
        BiometricVerification verification = repository.save(mapper.toEntity(request));
        EkYcEmbededResponse embeded = ekycClient.createEmbededRequest(buildEkYcRequest(verification));
        verification.setEkycRequestId(embeded.requestId());
        verification.setEmbededToken(embeded.embededToken());
        verification.setEmbededExpiry(embeded.embededExpiry());
        verification.setRequestUrl(embeded.requestUrl());
        return mapper.toResponse(repository.save(verification));
    }

    @Override
    @Transactional(readOnly = true)
    public BiometricVerificationResponse getById(UUID id) {
        return mapper.toResponse(getEntityById(id));
    }

    @Override
    public void handleCallback(BiometricCallbackPayload payload) {
        log.info("Processing callback for eKYC requestId={}, status={}, result={}, statusCode={}",
                payload.requestId(), payload.status(), payload.result(), payload.statusCode());

        BiometricVerification verification = repository.findByEkycRequestId(payload.requestId())
                .orElseThrow(() -> {
                    log.error("BiometricVerification record not found for eKYC request_id {}", payload.requestId());
                    return new ResourceNotFoundException(
                            "BiometricVerification for eKYC request_id " + payload.requestId());
                });

        if (verification.getStatus() != BiometricVerificationStatus.PENDING) {
            log.warn("BiometricVerification {} already in status {}, ignoring callback",
                    verification.getId(), verification.getStatus());
            return;
        }

        try {
            verification.setStatus(BiometricVerificationStatus.from(payload.status()));
        } catch (Exception e) {
            log.error("Failed to map status '{}' to BiometricVerificationStatus: {}", payload.status(), e.getMessage());
            throw e;
        }

        verification.setResult(payload.result());
        verification.setStatusCode(payload.statusCode());
        verification.setRemainingAttempts(payload.remainingAttempts());
        BiometricVerification resolved = repository.save(verification);
        log.info("Saved BiometricVerification {}: new status={}, result={}",
                resolved.getId(), resolved.getStatus(), resolved.getResult());

        try {
            eventPublisher.publish(RabbitConfig.BIOMETRIC_VERIFICATION_RESOLVED_KEY, Map.of(
                    "verificationId", resolved.getId().toString(),
                    "policyNumber", resolved.getPolicyNumber(),
                    "status", resolved.getStatus().name(),
                    "result", resolved.getResult()));
            log.info("Published BIOMETRIC_VERIFICATION_RESOLVED event for verification {}", resolved.getId());
        } catch (Exception e) {
            log.error("Failed to publish BIOMETRIC_VERIFICATION_RESOLVED event for verification {}: {}",
                    resolved.getId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public HttpStatusCode resend(UUID id) {
        BiometricVerification verification = getEntityById(id);
        if (verification.getStatus() != BiometricVerificationStatus.PENDING) {
            throw new IllegalStateException("Biometric verification has already been resolved");
        }
        if (verification.getEkycRequestId() == null) {
            throw new IllegalStateException("Biometric verification has no eKYC request id yet");
        }
        return ekycClient.resendCallback(verification.getEkycRequestId());
    }

    @Override
    @Transactional(readOnly = true)
    public BiometricVerification getEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BiometricVerification", id));
    }

    private EkYcCreateRequest buildEkYcRequest(BiometricVerification verification) {
        return new EkYcCreateRequest(
                properties.getNotificationCallbackUrl(),
                EKYC_REASON,
                EKYC_AGENT_ID_NUMBER,
                EKYC_AGENT_ID_TYPE,
                verification.getSubjectIdNumber(),
                verification.getSubjectIdType(),
                verification.getId().toString(),
                EKYC_EXPIRES_IN_SECONDS,
                EKYC_SERVICE_ID,
                EKYC_TOTAL_ATTEMPTS,
                EKYC_REQUEST_MODE,
                EKYC_POOR_QUALITY_RESULT_ATTEMPTS,
                EKYC_LOCATION_NAME,
                verification.getWorkstationId(),
                EKYC_DEVICE_ID,
                EKYC_DEVICE_NAME
        );
    }
}
