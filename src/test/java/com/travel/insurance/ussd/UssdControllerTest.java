package com.travel.insurance.ussd;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.ussd.controller.UssdController;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import com.travel.insurance.ussd.service.UssdService;
import com.travel.insurance.ussd.utils.UssdSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UssdController.class)
class UssdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UssdSessionManager sessionManager;

    @MockBean
    private UssdService ussdService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    void handleFormEncodedCallbackReturnsUssdResponse() throws Exception {
        UssdSession session = new UssdSession();
        when(sessionManager.buildSessionKey(any())).thenReturn("ussd:session:254700000000:sess-1");
        when(sessionManager.findOrCreateSession(anyString(), any())).thenReturn(session);
        when(sessionManager.extractAndTrackInput(any(), any())).thenReturn("");
        when(ussdService.processSessionStep(any(), anyString()))
                .thenReturn(new UssdResponse("Welcome to Inbound Travel Medical Insurance.\n1. Find Hospital\n2. Feedback", "CON"));
        when(sessionManager.formatWireText(any())).thenReturn("CON Welcome to Inbound Travel Medical Insurance.\n1. Find Hospital\n2. Feedback");

        mockMvc.perform(post("/ussd/handle")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("sessionId", "sess-1")
                        .param("phoneNumber", "254700000000")
                        .param("text", "")
                        .param("serviceCode", "*384#"))
                .andExpect(status().isOk())
                .andExpect(content().string("CON Welcome to Inbound Travel Medical Insurance.\n1. Find Hospital\n2. Feedback"));
    }

    @Test
    @WithMockUser
    void handleJsonCallbackReturnsUssdResponse() throws Exception {
        UssdSession session = new UssdSession();
        when(sessionManager.buildSessionKey(any())).thenReturn("ussd:session:254700000000:sess-1");
        when(sessionManager.findOrCreateSession(anyString(), any())).thenReturn(session);
        when(sessionManager.extractAndTrackInput(any(), any())).thenReturn("1");
        when(ussdService.processSessionStep(any(), anyString()))
                .thenReturn(new UssdResponse("1. Check Hospital by Name", "CON"));
        when(sessionManager.formatWireText(any())).thenReturn("CON 1. Check Hospital by Name");

        String json = "{\"sessionId\":\"sess-1\",\"msisdn\":\"254700000000\",\"text\":\"1\",\"serviceCode\":\"*384#\"}";

        mockMvc.perform(post("/ussd/handle")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("CON 1. Check Hospital by Name"));
    }
}
