package com.travel.insurance.user.dto;

import com.travel.insurance.user.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Set<Role> roles,
        UUID organizationId,
        Instant createdDate,
        Instant updatedDate
) {
}
