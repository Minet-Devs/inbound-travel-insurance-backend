package com.travel.insurance.claim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID claimId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();
    private final UUID visitorId = UUID.randomUUID();
    private final UUID insurerId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    private ClaimResponse sampleClaim() {
        return new ClaimResponse(claimId, policyId, benefitId, null, null, visitorId, null,
                insurerId, null, new BigDecimal("50000.00"), "KES", new BigDecimal("385.00"),
                new BigDecimal("0.0077"), "USD", LocalDateTime.now(), null, "Hospital stay",
                "Paracetamol 500mg twice daily",
                Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID()), Set.of(invoiceId), List.of(),
                Set.of(documentId), null, ClaimStatus.SUBMITTED, Instant.now(), Instant.now());
    }

    private ClaimRequest sampleRequest() {
        return new ClaimRequest(policyId, benefitId, null, null, visitorId,
                new BigDecimal("50000.00"), "Hospital stay", "Paracetamol 500mg twice daily",
                Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID()), Set.of(invoiceId), Set.of(documentId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreatedWithAugmentedFields() throws Exception {
        when(claimService.create(any(ClaimRequest.class))).thenReturn(sampleClaim());

        mockMvc.perform(post("/api/v1/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.insurerId").value(insurerId.toString()))
                .andExpect(jsonPath("$.prescription").value("Paracetamol 500mg twice daily"))
                .andExpect(jsonPath("$.invoiceIds[0]").value(invoiceId.toString()))
                .andExpect(jsonPath("$.documentIds[0]").value(documentId.toString()))
                .andExpect(jsonPath("$.claimedAmountBase").value(385.00))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsClaim() throws Exception {
        when(claimService.getById(claimId)).thenReturn(sampleClaim());

        mockMvc.perform(get("/api/v1/claims/{id}", claimId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(claimId.toString()))
                .andExpect(jsonPath("$.prescription").value("Paracetamol 500mg twice daily"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listReturnsClaims() throws Exception {
        when(claimService.list(any()))
                .thenReturn(new PageImpl<>(List.of(sampleClaim()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].visitorId").value(visitorId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void decideAppliesDecision() throws Exception {
        when(claimService.decide(any(), any())).thenReturn(sampleClaim());

        mockMvc.perform(post("/api/v1/claims/{id}/decision", claimId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"approvedAmount\":40000.00,\"reason\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void attachInvoiceAttachesInvoiceToClaim() throws Exception {
        when(claimService.attachInvoice(any(), any())).thenReturn(sampleClaim());

        mockMvc.perform(put("/api/v1/claims/{id}/invoice", claimId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceId\":\"" + invoiceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(claimId.toString()))
                .andExpect(jsonPath("$.invoiceIds[0]").value(invoiceId.toString()));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/claims/{id}", claimId))
                .andExpect(status().isUnauthorized());
    }
}
