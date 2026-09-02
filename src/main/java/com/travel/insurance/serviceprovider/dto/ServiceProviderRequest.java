package com.travel.insurance.serviceprovider.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceProviderRequest(
        @NotBlank String name,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        String address,
        UUID organizationId,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude
) {
}
