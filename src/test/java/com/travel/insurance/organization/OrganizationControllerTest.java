package com.travel.insurance.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.organization.dto.OrganizationRequest;
import com.travel.insurance.organization.dto.OrganizationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID organizationId = UUID.randomUUID();

    private OrganizationResponse sampleResponse() {
        return new OrganizationResponse(organizationId, "Acme Ltd", OrganizationType.INSURER, "contact@acme.com",
                "0700000000", "123 Main St", "Nairobi", "https://cdn.example/acme.png", 123456L, "notify@acme.com",
                "smtp.acme.com", 587, "signature-data", Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser
    void getByIdReturnsOrganization() throws Exception {
        when(organizationService.getById(organizationId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/organizations/{id}", organizationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId.toString()))
                .andExpect(jsonPath("$.name").value("Acme Ltd"))
                .andExpect(jsonPath("$.organizationType").value("INSURER"))
                .andExpect(jsonPath("$.email").value("contact@acme.com"))
                .andExpect(jsonPath("$.city").value("Nairobi"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreated() throws Exception {
        when(organizationService.create(any(OrganizationRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/organizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrganizationRequest("Acme Ltd", OrganizationType.INSURER, "contact@acme.com",
                                        "0700000000", "123 Main St", "Nairobi", "https://cdn.example/acme.png",
                                        123456L, "notify@acme.com", "s3cr3t", "smtp.acme.com", 587,
                                        "signature-data"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Ltd"))
                .andExpect(jsonPath("$.logoUrl").value("https://cdn.example/acme.png"))
                .andExpect(jsonPath("$.policyToken").value(123456))
                .andExpect(jsonPath("$.notificationEmail").value("notify@acme.com"))
                .andExpect(jsonPath("$.host").value("smtp.acme.com"))
                .andExpect(jsonPath("$.port").value(587))
                .andExpect(jsonPath("$.esignature").value("signature-data"))
                .andExpect(jsonPath("$.notificationEmailPassword").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/organizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrganizationRequest("", null, "not-an-email", null, null, null, null, null, null,
                                        null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser
    void listFiltersByOrganizationType() throws Exception {
        when(organizationService.list(eq(OrganizationType.INSURER), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/organizations").param("organizationType", "INSURER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].organizationType").value("INSURER"));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/{id}", organizationId))
                .andExpect(status().isUnauthorized());
    }
}
