package com.travel.insurance.biometric.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EkYcAccessTokenRequest(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_secret") String clientSecret,
        @JsonProperty("grant_type") String grantType
) {
}
