package com.travel.insurance.serviceprovider.dto;

import java.time.Instant;
import java.util.UUID;

public record ServiceProviderResponse(
        UUID id,
        String name,
        String contactEmail,
        String contactPhone,
        String address,
        UUID organizationId,
        Instant createdDate,
        Instant updatedDate
) {
}
