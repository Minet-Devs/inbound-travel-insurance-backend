package com.travel.insurance.preauthorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreauthorizationController.class)
class PreauthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PreauthorizationService preauthorizationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID preauthorizationId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID visitorId = UUID.randomUUID();
    private final UUID icd11CodeId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();
    private final UUID serviceProviderId = UUID.randomUUID();

    private PreauthorizationResponse sampleResponse(PreauthorizationStatus status) {
        return new PreauthorizationResponse(
                preauthorizationId, policyId, UUID.randomUUID(), visitorId, "Jane Traveler",
                icd11CodeId, "1A00", "Cholera", benefitId, "Medical Expenses",
                serviceProviderId, "Aga Khan Hospital", null, null,
                new BigDecimal("500.00"), null, "X-ray", null, status,
                Instant.now(), Instant.now(), null, null, List.of(), false);
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void createReturnsCreated() throws Exception {
        when(preauthorizationService.create(any(PreauthorizationRequest.class)))
                .thenReturn(sampleResponse(PreauthorizationStatus.PENDING));

        PreauthorizationRequest request = new PreauthorizationRequest(
                policyId, visitorId, icd11CodeId, benefitId, serviceProviderId, null,
                new BigDecimal("500.00"), "X-ray", null);

        mockMvc.perform(post("/api/v1/preauthorizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.icd11CodeId").value(icd11CodeId.toString()));
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void createRejectsMissingVisitorId() throws Exception {
        PreauthorizationRequest request = new PreauthorizationRequest(
                policyId, null, icd11CodeId, benefitId, serviceProviderId, null,
                new BigDecimal("500.00"), "X-ray", null);

        mockMvc.perform(post("/api/v1/preauthorizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsPreauthorization() throws Exception {
        when(preauthorizationService.getById(preauthorizationId))
                .thenReturn(sampleResponse(PreauthorizationStatus.PENDING));

        mockMvc.perform(get("/api/v1/preauthorizations/{id}", preauthorizationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(preauthorizationId.toString()));
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void decideReturnsUpdatedPreauthorization() throws Exception {
        when(preauthorizationService.decide(eq(preauthorizationId), any(PreauthorizationDecisionRequest.class)))
                .thenReturn(sampleResponse(PreauthorizationStatus.APPROVED));

        PreauthorizationDecisionRequest request = new PreauthorizationDecisionRequest(
                PreauthorizationStatus.APPROVED, new BigDecimal("500.00"), "approved in full");

        mockMvc.perform(post("/api/v1/preauthorizations/{id}/decision", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/preauthorizations/{id}", preauthorizationId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/preauthorizations/{id}", preauthorizationId))
                .andExpect(status().isUnauthorized());
    }
}