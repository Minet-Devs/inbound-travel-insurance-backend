package com.travel.insurance.notification;

import com.travel.insurance.auth.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WelcomePackController.class)
class WelcomePackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitorActivatedNotificationListener documentGenerator;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    void returnsWelcomePackPdf() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        when(documentGenerator.getWelcomePackPdf()).thenReturn(pdf);

        mockMvc.perform(get("/api/v1/welcome-pack"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser
    void returns409WhenPackUnavailable() throws Exception {
        when(documentGenerator.getWelcomePackPdf()).thenThrow(new IllegalStateException("unavailable"));

        mockMvc.perform(get("/api/v1/welcome-pack"))
                .andExpect(status().isConflict());
    }
}
