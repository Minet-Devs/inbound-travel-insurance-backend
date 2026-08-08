package com.travel.insurance.benefit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BenefitRepository extends JpaRepository<Benefit, UUID> {

    Page<Benefit> findAllByPolicyId(UUID policyId, Pageable pageable);

    List<Benefit> findAllByPolicyId(UUID policyId);

    boolean existsByPolicyIdAndBenefitType(UUID policyId, BenefitType benefitType);

    boolean existsByPolicyIdAndBenefitTypeAndIdNot(UUID policyId, BenefitType benefitType, UUID id);
}
