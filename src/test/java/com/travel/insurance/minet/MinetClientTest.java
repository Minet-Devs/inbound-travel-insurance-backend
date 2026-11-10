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
    private static final String CLAIMS_URL = "http://minet/claims/inbound/create";
    private static final String SCHEME_ID = "3b83907b-0bf8-47cb-ab00-8055c050f7fe";

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
        properties.setClaimsUrl(CLAIMS_URL);
        properties.setInboundSchemeId(SCHEME_ID);
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

    private final MinetClaimRequest claimRequest = new MinetClaimRequest(
            "AGA KHAN HOSPITAL NAIROBI", "MSP-1001", "Medical Expenses", "7777", "2026-11-06", "Outpatient",
            "NOINJ", "No Injury", "2026-11-06", new java.math.BigDecimal("100000"), "INV030212983", "2026-11-06",
            new java.math.BigDecimal("100000"), "Grouped Service", "Grouped Department",
            new java.math.BigDecimal("769.88"), new java.math.BigDecimal("0.008"),
            "2eadeb78-4028-465a-b3c4-33e51215a09e",
            java.util.List.of(
                    new MinetClaimRequest.Invoice("INV030212983", "2026-11-06", new java.math.BigDecimal("60000"), "KES"),
                    new MinetClaimRequest.Invoice("INV030212984", "2026-11-07", new java.math.BigDecimal("40000"), "KES")));

    @Test
    void postsClaimWithSchemePublicIdQueryAndBearerToken() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"tok123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(CLAIMS_URL + "?schemePublicId=" + SCHEME_ID))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok123"))
                .andExpect(jsonPath("$.providerName").value("AGA KHAN HOSPITAL NAIROBI"))
                .andExpect(jsonPath("$.providerId").value("MSP-1001"))
                .andExpect(jsonPath("$.passportNumber").value("7777"))
                .andExpect(jsonPath("$.claimDate").value("2026-11-06"))
                .andExpect(jsonPath("$.amountClaimed").value(100000))
                .andExpect(jsonPath("$.claimAmountUsd").value(769.88))
                .andExpect(jsonPath("$.exchangeRate").value(0.008))
                .andExpect(jsonPath("$.service").value("Grouped Service"))
                .andExpect(jsonPath("$.department").value("Grouped Department"))
                .andExpect(jsonPath("$.inboundClaimReference").value("2eadeb78-4028-465a-b3c4-33e51215a09e"))
                .andExpect(jsonPath("$.invoices.length()").value(2))
                .andExpect(jsonPath("$.invoices[0].invoiceNumber").value("INV030212983"))
                .andExpect(jsonPath("$.invoices[1].invoiceAmount").value(40000))
                .andExpect(jsonPath("$.invoices[1].currency").value("KES"))
                .andRespond(withSuccess("{\"success\":true,\"claimId\":\"c1\",\"claimNumber\":\"CLM-1\"}", MediaType.APPLICATION_JSON));

        client.sendClaim(claimRequest);

        server.verify();
    }

    @Test
    void failsWhenClaimPostReturnsError() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"tok123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(CLAIMS_URL + "?schemePublicId=" + SCHEME_ID)).andRespond(withServerError());

        assertThatThrownBy(() -> client.sendClaim(claimRequest))
                .isInstanceOf(MinetClientException.class)
                .hasMessageContaining("Minet claim sync failed")
                .hasMessageContaining("500");
    }

    @Test
    void failsWhenClaimResponseBodyHasSuccessFalseEvenWith200() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"tok123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(CLAIMS_URL + "?schemePublicId=" + SCHEME_ID))
                .andRespond(withSuccess("{\"rowNumber\":0,\"passportNumber\":\"7777\",\"success\":false,"
                        + "\"claimId\":null,\"message\":\"Unexpected error: null\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.sendClaim(claimRequest))
                .isInstanceOf(MinetClientException.class)
                .hasMessageContaining("Unexpected error: null");
    }

    @Test
    void failsWhenClaimResponseBodyIsEmpty() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"tok123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(CLAIMS_URL + "?schemePublicId=" + SCHEME_ID)).andRespond(withSuccess());

        assertThatThrownBy(() -> client.sendClaim(claimRequest))
                .isInstanceOf(MinetClientException.class);
    }
}
