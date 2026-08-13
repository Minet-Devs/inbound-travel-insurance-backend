package com.travel.insurance.medicalservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MedicalServiceRequest(
        @NotBlank String name,
        @NotNull UUID departmentId
) {
}