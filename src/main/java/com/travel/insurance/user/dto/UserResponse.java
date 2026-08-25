package com.travel.insurance.user.dto;

import com.travel.insurance.user.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Role role,
        UUID organizationId,
        String organizationName,
        UUID serviceProviderId,
        UUID insurerId,
        Instant createdDate,
        Instant updatedDate
) {
}
