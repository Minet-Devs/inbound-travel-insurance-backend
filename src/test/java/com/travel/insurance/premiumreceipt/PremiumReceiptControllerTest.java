package com.travel.insurance.premiumreceipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptPatchRequest;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PremiumReceiptController.class)
class PremiumReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PremiumReceiptService premiumReceiptService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private PremiumReceiptResponse sampleResponse() {
        return new PremiumReceiptResponse(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                new BigDecimal("44"), new BigDecimal("0.0001"), new BigDecimal("0.0005"), new BigDecimal("40"),
                new BigDecimal("0.001"), Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser
    void getReturnsPremiumReceipt() throws Exception {
        when(premiumReceiptService.get()).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/premium-receipts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPremium").value(44))
                .andExpect(jsonPath("$.stampDuty").value(40));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchReturnsUpdatedPremiumReceipt() throws Exception {
        when(premiumReceiptService.patch(any(PremiumReceiptPatchRequest.class))).thenReturn(sampleResponse());
        PremiumReceiptPatchRequest patchRequest = new PremiumReceiptPatchRequest(null, null, null, null, null);

        mockMvc.perform(patch("/api/v1/premium-receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPremium").value(44));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchRejectsOutOfRangePercentage() throws Exception {
        PremiumReceiptPatchRequest patchRequest =
                new PremiumReceiptPatchRequest(null, new BigDecimal("1.5"), null, null, null);

        mockMvc.perform(patch("/api/v1/premium-receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/premium-receipts"))
                .andExpect(status().isUnauthorized());
    }
}
