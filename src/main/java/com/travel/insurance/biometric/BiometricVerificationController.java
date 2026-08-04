package com.travel.insurance.biometric;

import com.travel.insurance.biometric.dto.BiometricVerificationRequest;
import com.travel.insurance.biometric.dto.BiometricVerificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/biometric-verifications")
@RequiredArgsConstructor
public class BiometricVerificationController {

    private final BiometricVerificationService biometricVerificationService;

    @PostMapping
    public ResponseEntity<BiometricVerificationResponse> create(
            @Valid @RequestBody BiometricVerificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(biometricVerificationService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BiometricVerificationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(biometricVerificationService.getById(id));
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<Void> resend(@PathVariable UUID id) {
        return ResponseEntity.status(biometricVerificationService.resend(id)).build();
    }
}
