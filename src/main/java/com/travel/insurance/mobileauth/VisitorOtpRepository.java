package com.travel.insurance.mobileauth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VisitorOtpRepository extends JpaRepository<VisitorOtp, UUID> {

    Optional<VisitorOtp> findFirstByEmailOrderByCreatedDateDesc(String email);
}
