package com.travel.insurance.biometric.client;

import com.travel.insurance.config.EkYcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class EkYcClient {

    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    private final EkYcProperties properties;
    private final RestClient.Builder restClientBuilder;

    public EkYcEmbededResponse createEmbededRequest(EkYcCreateRequest request) {
        return restClientBuilder.build().post()
                .uri(properties.getVerificationUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new EkYcClientException("eKYC create-request failed with status " + res.getStatusCode());
                })
                .body(EkYcEmbededResponse.class);
    }

    public HttpStatusCode resendCallback(String requestId) {
        return restClientBuilder.build().post()
                .uri(properties.getCallbackResendUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EkYcResendRequest(requestId))
                .exchange((req, res) -> {
                    res.close();
                    return res.getStatusCode();
                });
    }

    private String fetchAccessToken() {
        EkYcAccessTokenResponse response = restClientBuilder.build().post()
                .uri(properties.getAccessTokenUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EkYcAccessTokenRequest(
                        properties.getClientId(), properties.getClientSecret(), GRANT_TYPE_CLIENT_CREDENTIALS))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new EkYcClientException("eKYC access-token request failed with status " + res.getStatusCode());
                })
                .body(EkYcAccessTokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new EkYcClientException("eKYC access-token response did not contain an access_token");
        }
        return response.accessToken();
    }
}
