package com.travel.insurance.insurer.dto;

import java.time.Instant;
import java.util.UUID;

public record InsurerResponse(
        UUID id,
        String name,
        String contactEmail,
        String contactPhone,
        String address,
        String logoUrl,
        Long policyToken,
        Instant createdDate,
        Instant updatedDate
) {
}
