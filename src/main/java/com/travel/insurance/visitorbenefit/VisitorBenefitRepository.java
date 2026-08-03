package com.travel.insurance.visitorbenefit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VisitorBenefitRepository extends JpaRepository<VisitorBenefit, UUID> {

    Page<VisitorBenefit> findAllByVisitorId(UUID visitorId, Pageable pageable);

    List<VisitorBenefit> findAllByVisitorId(UUID visitorId);

    boolean existsByVisitorIdAndBenefitId(UUID visitorId, UUID benefitId);

    boolean existsByVisitorIdAndBenefitIdAndIdNot(UUID visitorId, UUID benefitId, UUID id);
}
