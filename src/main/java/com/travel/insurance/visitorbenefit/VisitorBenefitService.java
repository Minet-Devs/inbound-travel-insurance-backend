package com.travel.insurance.visitorbenefit;

import com.travel.insurance.visitorbenefit.dto.VisitorBenefitRequest;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface VisitorBenefitService {

    VisitorBenefitResponse create(VisitorBenefitRequest request);

    VisitorBenefitResponse getById(UUID id);

    Page<VisitorBenefitResponse> list(UUID visitorId, Pageable pageable);

    List<VisitorBenefitResponse> listAllByVisitor(UUID visitorId);

    VisitorBenefitResponse update(UUID id, VisitorBenefitRequest request);

    void delete(UUID id);

    VisitorBenefit getEntityById(UUID id);
}
