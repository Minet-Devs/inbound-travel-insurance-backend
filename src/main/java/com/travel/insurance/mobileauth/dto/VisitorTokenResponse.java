package com.travel.insurance.mobileauth.dto;

public record VisitorTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {

    public static VisitorTokenResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new VisitorTokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
