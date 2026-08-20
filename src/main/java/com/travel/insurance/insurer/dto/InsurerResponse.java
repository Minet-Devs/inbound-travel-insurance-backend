package com.travel.insurance.insurer.dto;

import java.time.Instant;
import java.util.UUID;

public record InsurerResponse(
        UUID id,
        UUID policyId,
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
        UUID organizationId,
        Instant createdDate,
        Instant updatedDate
) {
}
