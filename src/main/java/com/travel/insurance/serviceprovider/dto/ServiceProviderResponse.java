package com.travel.insurance.serviceprovider.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceProviderResponse(
        UUID id,
        String name,
        String contactEmail,
        String contactPhone,
        String address,
        UUID organizationId,
        BigDecimal longitude,
        BigDecimal latitude,
        Instant createdDate,
        Instant updatedDate
) {
}
