package com.travel.insurance.report;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.report.dto.ClaimReceiptResponse;
import com.travel.insurance.report.dto.ClaimInvoiceGroup;
import com.travel.insurance.report.dto.ProviderClaimReportResponse;
import com.travel.insurance.report.dto.ProviderClaimReportSummary;
import com.travel.insurance.report.dto.ProviderClaimReportRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID claimId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void claimReceipt_returnsOkWithJson() throws Exception {
        ClaimReceiptResponse receipt = new ClaimReceiptResponse(
                claimId, claimId.toString(), "SUBMITTED", Instant.now(),
                "John Doe", "AB123456", LocalDate.of(1990, 5, 15), "Kenyan",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15),
                "Medical Expenses", "Nairobi Hospital", List.of(), List.of(), List.of(),
                new BigDecimal("15000.00"));

        when(reportService.getClaimReceipt(claimId)).thenReturn(receipt);

        mockMvc.perform(get("/api/v1/reports/claims/{claimId}", claimId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(claimId.toString()))
                .andExpect(jsonPath("$.visitorName").value("John Doe"))
                .andExpect(jsonPath("$.benefitName").value("Medical Expenses"))
                .andExpect(jsonPath("$.providerName").value("Nairobi Hospital"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void claimReceiptPdf_returnsPdfContent() throws Exception {
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46};
        when(reportService.generateClaimReceiptPdf(claimId)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/reports/claims/{claimId}/pdf", claimId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=claim-receipt-" + claimId + ".pdf"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void providerReport_returnsOkWithPaginatedResults() throws Exception {
        ProviderClaimReportSummary summary = new ProviderClaimReportSummary(
                1, new BigDecimal("50000.00"), BigDecimal.ZERO, Map.of("SUBMITTED", 1));
        ProviderClaimReportRow row = new ProviderClaimReportRow(
                claimId, claimId.toString(), "SUBMITTED", Instant.now(),
                "John Doe", "Medical", new BigDecimal("50000.00"), null);
        ProviderClaimReportResponse response = new ProviderClaimReportResponse(
                summary, List.of(row), 0, 20, 1);

        when(reportService.getProviderReport(eq(providerId), isNull(), isNull(), isNull(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/service-providers/{providerId}/claims", providerId)
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalClaims").value(1))
                .andExpect(jsonPath("$.claims[0].visitorName").value("John Doe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void providerReport_withStatusFilter() throws Exception {
        ProviderClaimReportSummary summary = new ProviderClaimReportSummary(
                0, BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
        ProviderClaimReportResponse response = new ProviderClaimReportResponse(
                summary, List.of(), 0, 20, 0);

        when(reportService.getProviderReport(eq(providerId), eq("APPROVED"), isNull(), isNull(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/service-providers/{providerId}/claims", providerId)
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalClaims").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void providerReportPdf_returnsPdfContent() throws Exception {
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46};
        when(reportService.generateProviderReportPdf(eq(providerId), isNull(), isNull(), isNull())).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/reports/service-providers/{providerId}/claims/pdf", providerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=provider-claims-report.pdf"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void providerReportExcel_returnsXlsxContent() throws Exception {
        byte[] xlsxBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(reportService.generateProviderReportExcel(eq(providerId), isNull(), isNull(), isNull())).thenReturn(xlsxBytes);

        mockMvc.perform(get("/api/v1/reports/service-providers/{providerId}/claims/excel", providerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=provider-claims-report.xlsx"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void providerReport_withDateRange() throws Exception {
        ProviderClaimReportSummary summary = new ProviderClaimReportSummary(
                0, BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
        ProviderClaimReportResponse response = new ProviderClaimReportResponse(
                summary, List.of(), 0, 20, 0);

        when(reportService.getProviderReport(eq(providerId), isNull(),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31)), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/service-providers/{providerId}/claims", providerId)
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalClaims").value(0));
    }

    @Test
    void allEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/reports/claims/{claimId}", claimId))
                .andExpect(status().isUnauthorized());
    }
}
