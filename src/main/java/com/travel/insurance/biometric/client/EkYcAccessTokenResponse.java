package com.travel.insurance.biometric.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EkYcAccessTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("token_type") String tokenType
) {
}
