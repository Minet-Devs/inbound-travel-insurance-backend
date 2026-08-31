package com.travel.insurance.organization.dto;

import com.travel.insurance.organization.OrganizationType;
import jakarta.validation.constraints.Email;

/**
 * Partial-update counterpart to {@link OrganizationRequest}: every field is
 * optional, and only the fields present (non-null) in the request are
 * applied — unlike {@code PUT}, omitted fields are left untouched.
 */
public record OrganizationPatchRequest(
        String name,
        OrganizationType organizationType,
        @Email String email,
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
