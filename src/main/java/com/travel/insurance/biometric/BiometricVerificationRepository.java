package com.travel.insurance.biometric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BiometricVerificationRepository extends JpaRepository<BiometricVerification, UUID> {

    Optional<BiometricVerification> findByEkycRequestId(String ekycRequestId);
}
