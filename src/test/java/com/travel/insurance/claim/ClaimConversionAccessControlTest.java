package com.travel.insurance.claim;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role checks for converting a pre-authorization into a claim. Imports {@link SecurityConfig} so that
 * {@code @EnableMethodSecurity} is active and the controller's {@code @PreAuthorize} rules apply.
 */
@WebMvcTest(ClaimController.class)
@Import(SecurityConfig.class)
class ClaimConversionAccessControlTest {

    private static final String BODY = "{\"invoiceNumber\":\"INV-001\",\"issueDate\":\"2026-10-02\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID preauthorizationId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanConvertPreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void contactCenterCanConvertPreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void providerUserCannotConvertPreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void conversionRequiresInvoiceNumber() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceNumber\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
