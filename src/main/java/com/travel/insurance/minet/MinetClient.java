package com.travel.insurance.minet;

import com.travel.insurance.config.MinetProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class MinetClient {

    private final MinetProperties properties;
    private final RestClient.Builder restClientBuilder;

    public void sendVisitor(MinetVisitorRequest request) {
        restClientBuilder.build().post()
                .uri(properties.getVisitorsUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new MinetClientException("Minet visitor sync failed with status " + res.getStatusCode());
                })
                .toBodilessEntity();
    }

    private String fetchAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", properties.getUsername());
        form.add("password", properties.getPassword());
        form.add("grant_type", "password");
        form.add("client_id", properties.getClientId());

        MinetTokenResponse response = restClientBuilder.build().post()
                .uri(properties.getTokenUrl())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + properties.getBasicAuthorization())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new MinetClientException("Minet access-token request failed with status " + res.getStatusCode());
                })
                .body(MinetTokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new MinetClientException("Minet access-token response did not contain an access_token");
        }
        return response.accessToken();
    }
}
