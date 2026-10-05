package com.travel.insurance.touristattraction.dto;

import java.time.Instant;
import java.util.UUID;

public record TouristAttractionResponse(
        UUID id,
        String name,
        String county,
        Instant createdDate,
        Instant updatedDate
) {
}
