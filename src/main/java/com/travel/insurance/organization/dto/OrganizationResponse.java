package com.travel.insurance.organization.dto;

import com.travel.insurance.organization.OrganizationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        OrganizationType organizationType,
        String email,
        String phoneNumber,
        String address,
        String city,
        String logoUrl,
        Long policyToken,
        String notificationEmail,
        String host,
        Integer port,
        String esignature,
        BigDecimal longitude,
        BigDecimal latitude,
        Instant createdDate,
        Instant updatedDate
) {
}
