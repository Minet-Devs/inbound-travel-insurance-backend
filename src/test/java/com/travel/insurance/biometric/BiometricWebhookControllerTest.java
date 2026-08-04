package com.travel.insurance.biometric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.config.EkYcProperties;
import com.travel.insurance.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BiometricWebhookController.class)
@Import(SecurityConfig.class)
class BiometricWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BiometricVerificationService biometricVerificationService;

    @MockBean
    private SecureHashVerifier secureHashVerifier;

    @MockBean
    private EkYcProperties properties;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void acceptsValidCallbackFromAllowedIp() throws Exception {
        when(properties.getCallbackAllowedIps()).thenReturn(java.util.List.of("167.71.128.93"));
        when(secureHashVerifier.isValid(anyCallback())).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/biometric-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr("167.71.128.93");
                            return request;
                        })
                        .content(objectMapper.writeValueAsString(samplePayload())))
                .andExpect(status().isOk());

        verify(biometricVerificationService).handleCallback(anyCallback());
    }

    @Test
    void acceptsValidCallbackFromIpv6MappedAddress() throws Exception {
        when(properties.getCallbackAllowedIps()).thenReturn(java.util.List.of("167.71.128.93"));
        when(secureHashVerifier.isValid(anyCallback())).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/biometric-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr("::ffff:167.71.128.93");
                            return request;
                        })
                        .content(objectMapper.writeValueAsString(samplePayload())))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsCallbackFromDisallowedIp() throws Exception {
        when(properties.getCallbackAllowedIps()).thenReturn(java.util.List.of("167.71.142.137"));

        mockMvc.perform(post("/api/v1/webhooks/biometric-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.9");
                            return request;
                        })
                        .content(objectMapper.writeValueAsString(samplePayload())))
                .andExpect(status().isForbidden());

        verify(biometricVerificationService, never()).handleCallback(anyCallback());
    }

    @Test
    void rejectsCallbackWithInvalidSecureHash() throws Exception {
        when(properties.getCallbackAllowedIps()).thenReturn(java.util.List.of("167.71.128.93"));
        when(secureHashVerifier.isValid(anyCallback())).thenReturn(false);

        mockMvc.perform(post("/api/v1/webhooks/biometric-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr("167.71.128.93");
                            return request;
                        })
                        .content(objectMapper.writeValueAsString(samplePayload())))
                .andExpect(status().isUnauthorized());

        verify(biometricVerificationService, never()).handleCallback(anyCallback());
    }

    private BiometricCallbackPayload anyCallback() {
        return org.mockito.ArgumentMatchers.any(BiometricCallbackPayload.class);
    }

    private BiometricCallbackPayload samplePayload() {
        return new BiometricCallbackPayload(
                "529fd955-0000-0000-0000-000000000001",
                "prism-verification-uuid",
                "accepted",
                "match",
                "DICP2000",
                3,
                "hash");
    }
}
