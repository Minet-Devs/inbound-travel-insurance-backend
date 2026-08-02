package com.travel.insurance.insurer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InsurerRequest(
        @NotBlank String name,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        String address
) {
}
