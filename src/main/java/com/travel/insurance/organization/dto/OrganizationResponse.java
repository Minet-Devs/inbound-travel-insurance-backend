package com.travel.insurance.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String email,
        String phoneNumber,
        String address,
        String city,
        Instant createdDate,
        Instant updatedDate
) {
}
