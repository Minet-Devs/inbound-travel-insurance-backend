package com.travel.insurance.serviceprovider.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceProviderNearbyResponse(
        UUID id,
        String name,
        BigDecimal longitude,
        BigDecimal latitude,
        String contactEmail,
        String contactPhone
) {
}
