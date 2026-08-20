package com.travel.insurance.insurer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InsurerRequest(
        @NotBlank String name,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        String address,
        String logoUrl,
        Long policyToken,
        @Email String notificationEmail,
        String notificationEmailPassword,
        String host,
        Integer port,
        String esignature
) {
}
