package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface BenefitService {

    BenefitResponse create(BenefitRequest request);

    BenefitResponse getById(UUID id);

    Page<BenefitResponse> list(UUID policyId, Pageable pageable);

    BenefitResponse update(UUID id, BenefitRequest request);

    void delete(UUID id);

    Benefit getEntityById(UUID id);

    void drawDown(UUID benefitId, BigDecimal amount);
}
