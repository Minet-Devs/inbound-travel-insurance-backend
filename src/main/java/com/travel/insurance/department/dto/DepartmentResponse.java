package com.travel.insurance.department.dto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        Instant createdDate,
        Instant updatedDate
) {
}