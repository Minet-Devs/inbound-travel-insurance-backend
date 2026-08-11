package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.benefit.dto.BenefitTypeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BenefitService {

    List<BenefitTypeResponse> listBenefitTypes();

    /**
     * Provisions the fixed {@link BenefitType} catalog onto a policy, each with
     * its mandated fixed limit. Idempotent: types already present on the policy
     * are left untouched. Invoked when a policy is created.
     */
    List<BenefitResponse> provisionFixedBenefits(UUID policyId);

    BenefitResponse getById(UUID id);

    Page<BenefitResponse> list(UUID policyId, Pageable pageable);

    List<BenefitResponse> listAllByPolicy(UUID policyId);

    Map<UUID, BenefitType> typesByIds(Collection<UUID> benefitIds);

    Benefit getEntityById(UUID id);
}
