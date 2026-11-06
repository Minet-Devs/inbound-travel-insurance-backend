package com.travel.insurance.minet;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MinetTokenResponse(@JsonProperty("access_token") String accessToken) {
}
