package com.travel.insurance.serviceprovider.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ServiceProviderRequest(
        @NotBlank String name,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        String address,
        String country
) {
}
