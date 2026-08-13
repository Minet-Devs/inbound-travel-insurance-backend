package com.travel.insurance.department.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(
        @NotBlank String name
) {
}