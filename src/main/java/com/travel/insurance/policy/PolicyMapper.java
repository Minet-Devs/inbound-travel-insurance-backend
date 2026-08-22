package com.travel.insurance.policy;

import com.travel.insurance.policy.dto.PolicyRequest;
import com.travel.insurance.policy.dto.PolicyResponse;
import org.springframework.stereotype.Component;

@Component
public class PolicyMapper {

    public Policy toEntity(PolicyRequest request) {
        Policy policy = new Policy();
        updateEntity(policy, request);
        return policy;
    }

    public void updateEntity(Policy policy, PolicyRequest request) {
        policy.setInsurerId(request.insurerId());
        policy.setStatus(request.status() != null ? request.status() : PolicyStatus.ACTIVE);
    }

    public PolicyResponse toResponse(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getInsurerId(),
                policy.getStatus(),
                policy.getCreatedDate(),
                policy.getUpdatedDate()
        );
    }
}
