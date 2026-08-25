package com.travel.insurance.preauthorization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PreauthorizationRepository extends JpaRepository<Preauthorization, UUID> {

    Page<Preauthorization> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);

    Page<Preauthorization> findAllByPolicyId(UUID policyId, Pageable pageable);

    Page<Preauthorization> findAllByInsurerId(UUID insurerId, Pageable pageable);

    Page<Preauthorization> findAllByServiceProviderIdAndInsurerId(
            UUID serviceProviderId, UUID insurerId, Pageable pageable);
}
