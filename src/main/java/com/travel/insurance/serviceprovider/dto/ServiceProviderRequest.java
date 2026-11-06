package com.travel.insurance.serviceprovider.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceProviderRequest(
        @NotBlank String name,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        String address,
        @NotBlank String county,
        UUID organizationId,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        /** Provider id in Minet's system. On update: omitted/null leaves it unchanged, blank clears it. */
        @Size(max = 255) String minetServiceProviderId
) {

    /** Request without a Minet id (leaves any stored Minet id untouched on update). */
    public ServiceProviderRequest(String name, String contactEmail, String contactPhone, String address,
                                  String county, UUID organizationId, BigDecimal longitude, BigDecimal latitude) {
        this(name, contactEmail, contactPhone, address, county, organizationId, longitude, latitude, null);
    }
}
