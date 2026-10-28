package com.travel.insurance.preauthorization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface PreauthorizationRepository extends JpaRepository<Preauthorization, UUID> {

    Page<Preauthorization> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);

    Page<Preauthorization> findAllByPolicyId(UUID policyId, Pageable pageable);

    Page<Preauthorization> findAllByInsurerId(UUID insurerId, Pageable pageable);

    Page<Preauthorization> findAllByVisitorId(UUID visitorId, Pageable pageable);

    boolean existsByVisitorIdAndServiceProviderIdAndBenefitIdAndTreatmentDateAndRequestedAmount(
            UUID visitorId, UUID serviceProviderId, UUID benefitId, LocalDate treatmentDate,
            BigDecimal requestedAmount);

    Page<Preauthorization> findAllByServiceProviderIdAndInsurerId(
            UUID serviceProviderId, UUID insurerId, Pageable pageable);
}
