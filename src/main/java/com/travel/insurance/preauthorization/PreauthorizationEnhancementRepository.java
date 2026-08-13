package com.travel.insurance.preauthorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PreauthorizationEnhancementRepository extends JpaRepository<PreauthorizationEnhancement, UUID> {

    Optional<PreauthorizationEnhancement> findByPreauthorizationId(UUID preauthorizationId);

    boolean existsByPreauthorizationId(UUID preauthorizationId);
}