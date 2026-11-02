package com.travel.insurance.minet;

import com.travel.insurance.config.MinetProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MinetClientTest {

    private static final String TOKEN_URL = "http://minet/token";
    private static final String VISITORS_URL = "http://minet/visitors";

    private MockRestServiceServer server;
    private MinetClient client;

    private final MinetVisitorRequest request = new MinetVisitorRequest(
            "SP003369", "GARIMA", "THAPLIYAL", null, "FEMALE",
            LocalDate.of(1994, 7, 1), LocalDate.of(2026, 11, 6),
            "mbashiavictor@gmail.com", "+254791531926", "IND");

    @BeforeEach
    void setUp() {
        MinetProperties properties = new MinetProperties();
        properties.setTokenUrl(TOKEN_URL);
        properties.setVisitorsUrl(VISITORS_URL);
        properties.setBasicAuthorization("basic-secret");
        properties.setClientId("dev-application");
        properties.setUsername("user@minet");
        properties.setPassword("pw");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MinetClient(properties, builder);
    }

    @Test
    void fetchesTokenWithBasicAuthThenPostsVisitorWithBearer() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic basic-secret"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(
                        "username=user%40minet&password=pw&grant_type=password&client_id=dev-application"))
                .andRespond(withSuccess("{\"access_token\":\"tok123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(VISITORS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok123"))
                .andExpect(jsonPath("$.memberNumber").value("SP003369"))
                .andExpect(jsonPath("$.dateOfBirth").value("1994-07-01"))
                .andExpect(jsonPath("$.dateJoined").value("2026-11-06"))
                .andExpect(jsonPath("$.residenceCountry").value("IND"))
                .andRespond(withSuccess());

        client.sendVisitor(request);

        server.verify();
    }

    @Test
    void failsWhenTokenResponseHasNoAccessToken() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.sendVisitor(request))
                .isInstanceOf(MinetClientException.class)
                .hasMessageContaining("access_token");
    }

    @Test
    void failsWhenVisitorPostReturnsError() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"tok123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(VISITORS_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.sendVisitor(request))
                .isInstanceOf(MinetClientException.class)
                .hasMessageContaining("500");
    }
}
