package com.travel.insurance.minet;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MinetClaimStatusController.class)
class MinetClaimStatusControllerTest {

    private static final String URL = "/api/v1/webhooks/minet/claim-status";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID claimId = UUID.randomUUID();

    private String body(String reference) {
        return "{\"eventId\":\"3f2b9c1e\",\"inboundClaimReference\":" + reference
                + ",\"claimId\":\"6af2ef28\",\"claimNumber\":\"CLM-000123\",\"previousStatus\":\"Submitted\","
                + "\"status\":\"Approved\",\"changedAt\":\"2026-11-10T09:15:00Z\","
                + "\"comments\":[\"resubmitted\",\"Approved in full\"],\"somethingNew\":1}";
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void appliesUpdateToTheClaimNamedByInboundClaimReference() throws Exception {
        when(claimService.applyMinetStatusUpdate(any(), any(), any(), any(), any())).thenReturn(true);

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"" + claimId + "\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(claimId.toString()))
                .andExpect(jsonPath("$.applied").value(true));

        verify(claimService).applyMinetStatusUpdate(eq(claimId), eq("3f2b9c1e"), eq("Approved"),
                eq(Instant.parse("2026-11-10T09:15:00Z")), eq(List.of("resubmitted", "Approved in full")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateEventReturns200WithAppliedFalse() throws Exception {
        when(claimService.applyMinetStatusUpdate(any(), any(), any(), any(), any())).thenReturn(false);

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"" + claimId + "\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingOrInvalidReferenceReturns400() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body("null")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body("\"not-a-uuid\"")))
                .andExpect(status().isBadRequest());
        verify(claimService, never()).applyMinetStatusUpdate(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unknownClaimReturns404() throws Exception {
        when(claimService.applyMinetStatusUpdate(any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Claim", claimId));

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"" + claimId + "\"")))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"" + claimId + "\"")))
                .andExpect(status().isUnauthorized());
    }
}
