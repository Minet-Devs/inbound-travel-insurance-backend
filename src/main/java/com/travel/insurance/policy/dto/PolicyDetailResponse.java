package com.travel.insurance.policy.dto;

import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.policy.PolicyStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PolicyDetailResponse(
        UUID id,
        String policyNumber,
        Set<UUID> insurerIds,
        LocalDate coverStartDate,
        LocalDate coverEndDate,
        PolicyStatus status,
        List<BenefitResponse> benefits,
        Instant createdDate,
        Instant updatedDate
) {

    public static PolicyDetailResponse of(PolicyResponse policy, List<BenefitResponse> benefits) {
        return new PolicyDetailResponse(
                policy.id(),
                policy.policyNumber(),
                policy.insurerIds(),
                policy.coverStartDate(),
                policy.coverEndDate(),
                policy.status(),
                benefits,
                policy.createdDate(),
                policy.updatedDate());
    }
}
