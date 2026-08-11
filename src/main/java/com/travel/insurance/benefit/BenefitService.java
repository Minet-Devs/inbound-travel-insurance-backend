package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface BenefitService {

    BenefitResponse create(BenefitRequest request);

    BenefitResponse getById(UUID id);

    Page<BenefitResponse> list(Pageable pageable);

    BenefitResponse update(UUID id, BenefitRequest request);

    void delete(UUID id);

    Benefit getEntityById(UUID id);

    /** Resolves benefit names for the given ids (used by cross-feature reads). */
    Map<UUID, String> namesByIds(Collection<UUID> benefitIds);
}
