package com.travel.insurance.claim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    Page<Claim> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);

    Page<Claim> findAllByPolicyId(UUID policyId, Pageable pageable);
}
