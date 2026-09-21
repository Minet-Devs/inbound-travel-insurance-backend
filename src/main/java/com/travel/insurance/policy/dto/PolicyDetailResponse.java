package com.travel.insurance.policy.dto;

import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.policy.PolicyStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PolicyDetailResponse(
        UUID id,
        UUID insurerId,
        String insurerName,
        PolicyStatus status,
        List<BenefitResponse> benefits,
        Instant createdDate,
        Instant updatedDate
) {

    public static PolicyDetailResponse of(PolicyResponse policy, String insurerName, List<BenefitResponse> benefits) {
        return new PolicyDetailResponse(
                policy.id(),
                policy.insurerId(),
                insurerName,
                policy.status(),
                benefits,
                policy.createdDate(),
                policy.updatedDate());
    }
}
