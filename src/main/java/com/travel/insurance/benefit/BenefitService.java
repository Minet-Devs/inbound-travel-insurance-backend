package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
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

    List<BenefitResponse> create(List<BenefitRequest> requests);

    BenefitResponse getById(UUID id);

    Page<BenefitResponse> list(UUID policyId, Pageable pageable);

    List<BenefitResponse> listAllByPolicy(UUID policyId);

    Map<UUID, BenefitType> typesByIds(Collection<UUID> benefitIds);

    BenefitResponse update(UUID id, BenefitRequest request);

    void delete(UUID id);

    Benefit getEntityById(UUID id);
}
