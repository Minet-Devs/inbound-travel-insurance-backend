package com.travel.insurance.mobileauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.mobileauth.dto.RequestVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VerifyVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VisitorTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileAuthController.class)
class MobileAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MobileAuthService mobileAuthService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    void requestOtpReturnsAccepted() throws Exception {
        RequestVisitorOtpRequest request = new RequestVisitorOtpRequest("visitor@example.com");
        doNothing().when(mobileAuthService).requestOtp(any(RequestVisitorOtpRequest.class));

        mockMvc.perform(post("/api/v1/mobile/auth/otp/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser
    void requestOtpRejectsInvalidBody() throws Exception {
        RequestVisitorOtpRequest request = new RequestVisitorOtpRequest("not-an-email");

        mockMvc.perform(post("/api/v1/mobile/auth/otp/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser
    void requestOtpPropagatesCooldownAsConflict() throws Exception {
        RequestVisitorOtpRequest request = new RequestVisitorOtpRequest("visitor@example.com");
        doThrow(new IllegalStateException("Please wait before requesting another code"))
                .when(mobileAuthService).requestOtp(any(RequestVisitorOtpRequest.class));

        mockMvc.perform(post("/api/v1/mobile/auth/otp/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void verifyOtpReturnsTokenOnSuccess() throws Exception {
        VerifyVisitorOtpRequest request = new VerifyVisitorOtpRequest("visitor@example.com", "123456");
        VisitorTokenResponse response = VisitorTokenResponse.bearer("access-token", "refresh-token", 900L);
        when(mobileAuthService.verifyOtp(any(VerifyVisitorOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/mobile/auth/otp/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @WithMockUser
    void verifyOtpReturnsConflictOnFailure() throws Exception {
        VerifyVisitorOtpRequest request = new VerifyVisitorOtpRequest("visitor@example.com", "123456");
        doThrow(new IllegalStateException("Invalid or expired code"))
                .when(mobileAuthService).verifyOtp(any(VerifyVisitorOtpRequest.class));

        mockMvc.perform(post("/api/v1/mobile/auth/otp/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid or expired code"));
    }

    @Test
    @WithMockUser
    void verifyOtpRejectsInvalidBody() throws Exception {
        VerifyVisitorOtpRequest request = new VerifyVisitorOtpRequest("visitor@example.com", "12");

        mockMvc.perform(post("/api/v1/mobile/auth/otp/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
