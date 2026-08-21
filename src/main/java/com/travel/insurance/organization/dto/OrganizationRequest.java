package com.travel.insurance.organization.dto;

import com.travel.insurance.organization.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
        String esignature
) {
}
