package com.travel.insurance.otp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.otp.dto.SendOtpRequest;
import com.travel.insurance.otp.dto.VerifyOtpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OtpController.class)
class OtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpService otpService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID serviceProviderId = UUID.randomUUID();

    @Test
    @WithMockUser
    void sendReturnsAccepted() throws Exception {
        SendOtpRequest request = new SendOtpRequest("visitor@example.com", serviceProviderId);
        doNothing().when(otpService).send(any(SendOtpRequest.class));

        mockMvc.perform(post("/api/v1/otps/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser
    void sendRejectsInvalidBody() throws Exception {
        SendOtpRequest request = new SendOtpRequest("not-an-email", null);

        mockMvc.perform(post("/api/v1/otps/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser
    void sendPropagatesUnknownServiceProviderAsNotFound() throws Exception {
        SendOtpRequest request = new SendOtpRequest("visitor@example.com", serviceProviderId);
        doThrow(new ResourceNotFoundException("ServiceProvider", serviceProviderId))
                .when(otpService).send(any(SendOtpRequest.class));

        mockMvc.perform(post("/api/v1/otps/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void verifyReturnsOkOnSuccess() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("visitor@example.com", serviceProviderId, "123456");
        doNothing().when(otpService).verify(any(VerifyOtpRequest.class));

        mockMvc.perform(post("/api/v1/otps/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void verifyReturnsConflictWhenExpired() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("visitor@example.com", serviceProviderId, "123456");
        doThrow(new IllegalStateException("Otp expired")).when(otpService).verify(any(VerifyOtpRequest.class));

        mockMvc.perform(post("/api/v1/otps/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Otp expired"));
    }

    @Test
    @WithMockUser
    void verifyRejectsInvalidBody() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("visitor@example.com", serviceProviderId, "12");

        mockMvc.perform(post("/api/v1/otps/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendWithoutAuthenticationIsRejected() throws Exception {
        SendOtpRequest request = new SendOtpRequest("visitor@example.com", serviceProviderId);

        mockMvc.perform(post("/api/v1/otps/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
