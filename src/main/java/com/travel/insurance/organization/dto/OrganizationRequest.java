package com.travel.insurance.organization.dto;

import com.travel.insurance.organization.OrganizationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrganizationRequest(
        @NotBlank String name,
        @NotNull OrganizationType organizationType,
        @NotBlank @Email String email,
        String phoneNumber,
        String address,
        String city,
        String logoUrl,
        Long policyToken,
        @Email String notificationEmail,
        String notificationEmailPassword,
        String host,
        Integer port,
        String esignature,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude
) {
}
