package com.travel.insurance.biometric.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EkYcResendRequest(
        @JsonProperty("request_id") String requestId
) {
}
