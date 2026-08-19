package com.travel.insurance.preauthorization;

import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PreauthorizationService {

    PreauthorizationResponse create(PreauthorizationRequest request);

    PreauthorizationResponse getById(UUID id);

    Page<PreauthorizationResponse> list(Pageable pageable);

    PreauthorizationResponse decide(UUID id, PreauthorizationDecisionRequest request);

    void delete(UUID id);

    Preauthorization getEntityById(UUID id);

    void markPreAuthorizationConvertedToClaim(UUID id);
}
