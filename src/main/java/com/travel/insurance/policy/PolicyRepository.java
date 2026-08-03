package com.travel.insurance.policy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    Page<Policy> findAllByInsurerIdsContains(UUID insurerId, Pageable pageable);

    boolean existsByPolicyNumber(String policyNumber);
}
