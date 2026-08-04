package com.travel.insurance.biometric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.biometric.dto.BiometricVerificationRequest;
import com.travel.insurance.biometric.dto.BiometricVerificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BiometricVerificationController.class)
class BiometricVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BiometricVerificationService biometricVerificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID verificationId = UUID.randomUUID();

    private BiometricVerificationResponse sampleResponse() {
        return new BiometricVerificationResponse(verificationId, "39289507", "citizen", "VMI-POL-001",
                "WS-NRB-014", "ekyc-req-1", "token-1", "2026-08-04T12:00:00Z",
                "https://ekyc.example/embeded?request_id=ekyc-req-1",
                BiometricVerificationStatus.PENDING, null, null, Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void createReturnsCreatedWithEmbededUrl() throws Exception {
        when(biometricVerificationService.create(any(BiometricVerificationRequest.class)))
                .thenReturn(sampleResponse());

        BiometricVerificationRequest request =
                new BiometricVerificationRequest("39289507", "citizen", "VMI-POL-001", "WS-NRB-014");
        mockMvc.perform(post("/api/v1/biometric-verifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ekycRequestId").value("ekyc-req-1"))
                .andExpect(jsonPath("$.requestUrl").value("https://ekyc.example/embeded?request_id=ekyc-req-1"));
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void createRejectsInvalidBody() throws Exception {
        BiometricVerificationRequest request = new BiometricVerificationRequest("", "", "", "");
        mockMvc.perform(post("/api/v1/biometric-verifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void getByIdReturnsVerification() throws Exception {
        when(biometricVerificationService.getById(verificationId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/biometric-verifications/{id}", verificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(verificationId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void resendPassesThroughEkYcStatus() throws Exception {
        when(biometricVerificationService.resend(verificationId)).thenReturn(HttpStatus.OK);

        mockMvc.perform(post("/api/v1/biometric-verifications/{id}/resend", verificationId).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/biometric-verifications/{id}", verificationId))
                .andExpect(status().isUnauthorized());
    }
}
