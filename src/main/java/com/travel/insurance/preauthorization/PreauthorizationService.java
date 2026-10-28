package com.travel.insurance.preauthorization;

import com.travel.insurance.preauthorization.dto.OfflineApproval;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PreauthorizationService {

    PreauthorizationResponse create(PreauthorizationRequest request);

    PreauthorizationResponse getById(UUID id);

    Page<PreauthorizationResponse> list(UUID insurerId, Pageable pageable);

    Page<PreauthorizationResponse> listByVisitor(UUID visitorId, Pageable pageable);

    PreauthorizationResponse decide(UUID id, PreauthorizationDecisionRequest request);

    /**
     * Creates an already-approved pre-authorization from an offline approval (approved amount equals the
     * requested amount) in its own transaction. Rejects an inactive policy and a duplicate of an existing
     * pre-authorization (same visitor, provider, benefit, treatment date and amount). Returns its id.
     */
    UUID createOfflineApproval(OfflineApproval approval);

    void delete(UUID id);

    Preauthorization getEntityById(UUID id);

    void markPreAuthorizationConvertedToClaim(UUID id);
}
