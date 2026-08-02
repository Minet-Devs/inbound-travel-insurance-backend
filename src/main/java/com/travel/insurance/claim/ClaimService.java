package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.ClaimDecisionRequest;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClaimService {

    ClaimResponse create(ClaimRequest request);

    ClaimResponse getById(UUID id);

    Page<ClaimResponse> list(Pageable pageable);

    ClaimResponse decide(UUID id, ClaimDecisionRequest request);

    ClaimResponse markPaid(UUID id);

    void delete(UUID id);
}
