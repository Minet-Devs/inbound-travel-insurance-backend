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
        String county,
        UUID organizationId,
        BigDecimal longitude,
        BigDecimal latitude,
        Instant createdDate,
        Instant updatedDate,
        String minetServiceProviderId
) {

    public ServiceProviderResponse(UUID id, String name, String contactEmail, String contactPhone, String address,
                                   String county, UUID organizationId, BigDecimal longitude, BigDecimal latitude,
                                   Instant createdDate, Instant updatedDate) {
        this(id, name, contactEmail, contactPhone, address, county, organizationId, longitude, latitude,
                createdDate, updatedDate, null);
    }
}
