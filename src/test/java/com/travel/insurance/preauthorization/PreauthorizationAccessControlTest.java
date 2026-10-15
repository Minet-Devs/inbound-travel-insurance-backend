package com.travel.insurance.preauthorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.config.SecurityConfig;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role checks for the pre-authorization endpoints. Imports {@link SecurityConfig} so that
 * {@code @EnableMethodSecurity} is active and the controller's {@code @PreAuthorize} rules apply.
 */
@WebMvcTest(PreauthorizationController.class)
@Import(SecurityConfig.class)
class PreauthorizationAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PreauthorizationService preauthorizationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID preauthorizationId = UUID.randomUUID();

    private String createBody() throws Exception {
        return objectMapper.writeValueAsString(new PreauthorizationRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, new BigDecimal("500.00"), "X-ray", null));
    }

    private String decisionBody() throws Exception {
        return objectMapper.writeValueAsString(new PreauthorizationDecisionRequest(
                PreauthorizationStatus.APPROVED, new BigDecimal("500.00"), "approved in full"));
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void contactCenterCanRaisePreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/preauthorizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void contactCenterCanDecidePreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/preauthorizations/{id}/decision", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionBody()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void contactCenterCannotDeletePreauthorization() throws Exception {
        mockMvc.perform(delete("/api/v1/preauthorizations/{id}", preauthorizationId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void providerUserCannotDecidePreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/preauthorizations/{id}/decision", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void insurerUserCannotRaisePreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/preauthorizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
    }
}
