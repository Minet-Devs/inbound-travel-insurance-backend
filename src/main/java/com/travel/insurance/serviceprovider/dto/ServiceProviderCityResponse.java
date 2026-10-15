package com.travel.insurance.serviceprovider.dto;

/** A service provider paired with its organization's city (from a city search). */
public record ServiceProviderCityResponse(
        ServiceProviderResponse provider,
        String city
) {
}
