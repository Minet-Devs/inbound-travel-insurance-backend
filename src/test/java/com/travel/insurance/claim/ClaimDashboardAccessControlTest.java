package com.travel.insurance.claim;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.claim.dto.ClaimDashboardSummary;
import com.travel.insurance.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
@Import(SecurityConfig.class)
class ClaimDashboardAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private ClaimExcelWriter claimExcelWriter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "KENYA_RE")
    void kenyaReCanReadClaimsDashboard() throws Exception {
        when(claimService.dashboardSummary(any(), any(), any()))
                .thenReturn(new ClaimDashboardSummary(0, BigDecimal.ZERO));
        when(claimService.distributionByInsurer(any(), any(), any())).thenReturn(List.of());
        when(claimService.distributionByServiceProvider(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/claims/dashboard/summary")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/claims/dashboard/by-insurer")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/claims/dashboard/by-service-provider")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "KENYA_RE")
    void kenyaReCannotReadOtherClaimEndpoints() throws Exception {
        when(claimService.list(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/claims")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VISITOR")
    void visitorCannotReadClaimsDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/claims/dashboard/summary")).andExpect(status().isForbidden());
    }
}
