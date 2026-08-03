package com.travel.insurance.policy;

import com.travel.insurance.policy.dto.PolicyRequest;
import com.travel.insurance.policy.dto.PolicyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PolicyService {

    PolicyResponse create(PolicyRequest request);

    PolicyResponse getById(UUID id);

    Page<PolicyResponse> list(Pageable pageable);

    PolicyResponse update(UUID id, PolicyRequest request);

    void delete(UUID id);

    Policy getEntityById(UUID id);
}
