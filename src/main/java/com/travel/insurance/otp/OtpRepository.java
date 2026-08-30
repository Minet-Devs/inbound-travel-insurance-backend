package com.travel.insurance.otp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {

    Optional<Otp> findFirstByEmailAndServiceProviderIdOrderByCreatedDateDesc(String email, UUID serviceProviderId);
}
