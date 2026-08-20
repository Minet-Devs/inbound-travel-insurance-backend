package com.travel.insurance.insurer.dto;

import com.travel.insurance.policy.dto.PolicyResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InsurerDetailResponse(
        UUID id,
        String name,
        String contactEmail,
        String contactPhone,
        String address,
        String logoUrl,
        Long policyToken,
        Long availablePolicies,
        String notificationEmail,
        String host,
        Integer port,
        String esignature,
        List<PolicyResponse> policies,
        Instant createdDate,
        Instant updatedDate
) {

    public static InsurerDetailResponse of(InsurerResponse insurer, List<PolicyResponse> policies) {
        return new InsurerDetailResponse(
                insurer.id(),
                insurer.name(),
                insurer.contactEmail(),
                insurer.contactPhone(),
                insurer.address(),
                insurer.logoUrl(),
                insurer.policyToken(),
                insurer.availablePolicies(),
                insurer.notificationEmail(),
                insurer.host(),
                insurer.port(),
                insurer.esignature(),
                policies,
                insurer.createdDate(),
                insurer.updatedDate());
    }
}