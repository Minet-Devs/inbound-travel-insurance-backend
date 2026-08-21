package com.travel.insurance.biometric;

import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.config.EkYcProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/biometric-verification")
@RequiredArgsConstructor
public class BiometricWebhookController {

    private final BiometricVerificationService biometricVerificationService;
    private final SecureHashVerifier secureHashVerifier;
    private final EkYcProperties properties;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody(required = false) BiometricCallbackPayload payload,
                                        HttpServletRequest request) {
        log.info("Received biometric verification webhook callback from ip={}: {}",
                request.getRemoteAddr(), payload);

        if (payload == null) {
            log.warn("Biometric webhook received empty or null payload from ip={}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (!secureHashVerifier.isValid(payload)) {
            log.warn("Biometric webhook secure hash validation failed for requestId={}, payload={}",
                    payload.requestId(), payload);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            biometricVerificationService.handleCallback(payload);
            log.info("Successfully processed biometric verification callback for requestId={}", payload.requestId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to process biometric verification callback for requestId={}: {}",
                    payload.requestId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
