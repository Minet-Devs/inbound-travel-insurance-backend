package com.travel.insurance.medicalservice.dto;

import java.time.Instant;
import java.util.UUID;

public record MedicalServiceResponse(
        UUID id,
        String name,
        UUID departmentId,
        String departmentName,
        Instant createdDate,
        Instant updatedDate
) {
}