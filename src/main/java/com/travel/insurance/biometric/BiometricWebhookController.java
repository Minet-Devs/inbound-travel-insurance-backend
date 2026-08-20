package com.travel.insurance.biometric;

import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.config.EkYcProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/biometric-verification")
@RequiredArgsConstructor
public class BiometricWebhookController {

    private static final String IPV4_MAPPED_PREFIX = "::ffff:";

    private final BiometricVerificationService biometricVerificationService;
    private final SecureHashVerifier secureHashVerifier;
    private final EkYcProperties properties;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody(required = false) BiometricCallbackPayload payload,
                                        HttpServletRequest request) {
//        if (!isAllowedIp(request)) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }
        if (payload == null || !secureHashVerifier.isValid(payload)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        biometricVerificationService.handleCallback(payload);
        return ResponseEntity.ok().build();
    }

    private boolean isAllowedIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (ip != null && ip.startsWith(IPV4_MAPPED_PREFIX)) {
            ip = ip.substring(IPV4_MAPPED_PREFIX.length());
        }
        return properties.getCallbackAllowedIps().contains(ip);
    }
}
