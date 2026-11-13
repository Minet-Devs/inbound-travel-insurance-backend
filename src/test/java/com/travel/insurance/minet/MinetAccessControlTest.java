package com.travel.insurance.minet;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MinetController.class, MinetClaimController.class, MinetClaimStatusController.class})
@Import(SecurityConfig.class)
class MinetAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinetVisitorSyncService minetVisitorSyncService;

    @MockBean
    private MinetClaimSyncService minetClaimSyncService;

    @MockBean
    private com.travel.insurance.claim.ClaimService claimService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanSync() throws Exception {
        when(minetVisitorSyncService.syncByPassportNumber("A1"))
                .thenReturn(new MinetSyncResponse(UUID.randomUUID(), true));

        mockMvc.perform(post("/api/v1/visitors/minet-sync/A1").with(csrf())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/visitors/minet-sync/A1").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanResendClaim() throws Exception {
        UUID claimId = UUID.randomUUID();
        when(minetClaimSyncService.syncClaim(claimId)).thenReturn(new MinetClaimSyncResponse(claimId, true));

        mockMvc.perform(post("/api/v1/claims/{id}/minet-sync", claimId).with(csrf())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void nonAdminCannotResendClaim() throws Exception {
        mockMvc.perform(post("/api/v1/claims/{id}/minet-sync", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanPostClaimStatusWebhook() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/minet/claim-status").with(csrf())
                        .contentType("application/json")
                        .content("{\"inboundClaimReference\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void nonAdminCannotPostClaimStatusWebhook() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/minet/claim-status").with(csrf())
                        .contentType("application/json")
                        .content("{\"inboundClaimReference\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanLookUpMemberByCertificate() throws Exception {
        mockMvc.perform(get("/api/v1/visitors/minet/certificate/ACME-2026-000123")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void nonAdminCannotLookUpMemberByCertificate() throws Exception {
        mockMvc.perform(get("/api/v1/visitors/minet/certificate/ACME-2026-000123"))
                .andExpect(status().isForbidden());
    }
}
