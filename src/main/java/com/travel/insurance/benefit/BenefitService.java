package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BenefitService {

    BenefitResponse create(BenefitRequest request);

    BenefitResponse getById(UUID id);

    Page<BenefitResponse> list(UUID policyId, Pageable pageable);

    List<BenefitResponse> listAllByPolicy(UUID policyId);

    BenefitResponse update(UUID id, BenefitRequest request);

    void delete(UUID id);

    Benefit getEntityById(UUID id);
}
