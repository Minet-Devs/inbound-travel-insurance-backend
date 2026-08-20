package com.travel.insurance.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OrganizationRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phoneNumber,
        String address,
        String city
) {
}
