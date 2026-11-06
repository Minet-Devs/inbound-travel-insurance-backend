package com.travel.insurance.biometric;

import com.travel.insurance.biometric.client.BiometricMicroserviceClient;
import com.travel.insurance.biometric.client.BiometricMicroserviceRequest;
import com.travel.insurance.biometric.client.BiometricMicroserviceResponse;
import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.biometric.dto.BiometricVerificationRequest;
import com.travel.insurance.biometric.dto.BiometricVerificationResponse;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BiometricVerificationServiceImpl implements BiometricVerificationService {

    private final BiometricVerificationRepository repository;
    private final BiometricVerificationMapper mapper;
    private final BiometricMicroserviceClient microserviceClient;
    private final EventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    /**
     * Runs without an ambient transaction on purpose: the microservice call can be slow, and
     * holding a pooled DB connection across it exhausts the Hikari pool. The DB writes are
     * short transactions either side of the call.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BiometricVerificationResponse create(BiometricVerificationRequest request) {
        BiometricVerification verification = transactionTemplate.execute(
                status -> repository.save(mapper.toEntity(request)));
        BiometricMicroserviceResponse embeded;
        try {
            embeded = microserviceClient.createVerification(
                    new BiometricMicroserviceRequest(
                            verification.getSubjectIdNumber(),
                            verification.getSubjectIdType(),
                            verification.getWorkstationId(),
                            verification.getId().toString()));
        } catch (RuntimeException ex) {
            // The row used to be rolled back with the surrounding transaction; undo it by hand.
            transactionTemplate.executeWithoutResult(status -> repository.deleteById(verification.getId()));
            throw ex;
        }
        verification.setEkycRequestId(embeded.requestId());
        verification.setEmbededToken(embeded.embededToken());
        verification.setEmbededExpiry(embeded.embededExpiry());
        verification.setRequestUrl(embeded.requestUrl());
        return mapper.toResponse(transactionTemplate.execute(status -> repository.save(verification)));
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public HttpStatusCode resend(UUID id) {
        BiometricVerification verification = getEntityById(id);
        if (verification.getStatus() != BiometricVerificationStatus.PENDING) {
            throw new IllegalStateException("Biometric verification has already been resolved");
        }
        if (verification.getEkycRequestId() == null) {
            throw new IllegalStateException("Biometric verification has no eKYC request id yet");
        }
        return microserviceClient.resendCallback(verification.getEkycRequestId());
    }

    @Override
    @Transactional(readOnly = true)
    public BiometricVerification getEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BiometricVerification", id));
    }
}