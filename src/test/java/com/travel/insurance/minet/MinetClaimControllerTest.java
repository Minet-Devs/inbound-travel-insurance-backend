package com.travel.insurance.minet;

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

@WebMvcTest(MinetClaimController.class)
class MinetClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinetClaimSyncService minetClaimSyncService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID claimId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncReturnsSentFlag() throws Exception {
        when(minetClaimSyncService.syncClaim(claimId)).thenReturn(new MinetClaimSyncResponse(claimId, true));

        mockMvc.perform(post("/api/v1/claims/{id}/minet-sync", claimId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(claimId.toString()))
                .andExpect(jsonPath("$.sentToMinet").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unknownClaimReturns404() throws Exception {
        when(minetClaimSyncService.syncClaim(claimId)).thenThrow(new ResourceNotFoundException("Claim", claimId));

        mockMvc.perform(post("/api/v1/claims/{id}/minet-sync", claimId).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void providerWithoutMinetIdReturns409() throws Exception {
        when(minetClaimSyncService.syncClaim(claimId)).thenThrow(new IllegalStateException("no minet id"));

        mockMvc.perform(post("/api/v1/claims/{id}/minet-sync", claimId).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void minetFailureReturns502() throws Exception {
        when(minetClaimSyncService.syncClaim(claimId)).thenThrow(new MinetClientException("status 500"));

        mockMvc.perform(post("/api/v1/claims/{id}/minet-sync", claimId).with(csrf()))
                .andExpect(status().isBadGateway());
    }
}
