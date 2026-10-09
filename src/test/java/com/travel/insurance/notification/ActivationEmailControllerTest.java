package com.travel.insurance.notification;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivationEmailController.class)
class ActivationEmailControllerTest {

    private static final String URL = "/api/v1/visitors/by-passport/resend-activation-email";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitorActivatedNotificationListener listener;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    void resendReturnsOkWhenEmailSent() throws Exception {
        UUID visitorId = UUID.randomUUID();
        when(listener.resendActivationEmailByPassportNumber("P1234567")).thenReturn(visitorId);

        mockMvc.perform(post(URL).param("passportNumber", "P1234567").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()));
    }

    @Test
    @WithMockUser
    void resendReturns404WhenVisitorUnknown() throws Exception {
        when(listener.resendActivationEmailByPassportNumber("X"))
                .thenThrow(new ResourceNotFoundException("Visitor", "X"));

        mockMvc.perform(post(URL).param("passportNumber", "X").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void resendReturns409WhenEmailCannotBeSent() throws Exception {
        when(listener.resendActivationEmailByPassportNumber("P1"))
                .thenThrow(new IllegalStateException("not active"));

        mockMvc.perform(post(URL).param("passportNumber", "P1").with(csrf()))
                .andExpect(status().isConflict());
    }
}
