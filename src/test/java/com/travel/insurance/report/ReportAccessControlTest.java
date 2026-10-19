package com.travel.insurance.report;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** KENYA_RE is read-only reporting; imports {@link SecurityConfig} so the real rules apply. */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "KENYA_RE")
    void kenyaReCanReadReports() throws Exception {
        when(reportService.generateVisitorReportExcel(any(), any(), any(), any())).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/v1/reports/visitors/excel"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "KENYA_RE")
    void kenyaReCannotReadOtherEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/visitors"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/claims/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminStillReachesNonReportEndpoints() throws Exception {
        // No controller is mapped for /api/v1/visitors in this slice; it just must not be blocked by security.
        mockMvc.perform(get("/api/v1/visitors"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }
}
