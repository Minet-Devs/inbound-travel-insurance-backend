package com.travel.insurance.claim;

import com.travel.insurance.common.domain.VisitType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.claim.dto.ClaimDashboardSummary;
import com.travel.insurance.claim.dto.ClaimListSummary;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.claim.dto.ClaimSearchCriteria;
import com.travel.insurance.claim.dto.CountryDiagnosisDistribution;
import com.travel.insurance.claim.dto.DiagnosisDistribution;
import com.travel.insurance.claim.dto.InsurerClaimDistribution;
import com.travel.insurance.claim.dto.ServiceProviderClaimDistribution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private ClaimExcelWriter claimExcelWriter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID claimId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();
    private final UUID visitorId = UUID.randomUUID();
    private final UUID insurerId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    private ClaimResponse sampleClaim() {
        return new ClaimResponse(claimId, policyId, benefitId, null, null, visitorId, null,
                insurerId, null, new BigDecimal("50000.00"), "KES", new BigDecimal("385.00"),
                new BigDecimal("0.0077"), "USD", LocalDateTime.now(), null, "Hospital stay",
                "Paracetamol 500mg twice daily",
                List.of(), List.of(), List.of(),
                Set.of(documentId), null, ClaimStatus.SUBMITTED, Instant.now(), Instant.now(), VisitType.OUTPATIENT, false, null, null);
    }

    private ClaimRequest sampleRequest() {
        return new ClaimRequest(policyId, benefitId, null, null, visitorId,
                new BigDecimal("50000.00"), "Hospital stay", "Paracetamol 500mg twice daily",
                Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID()), Set.of(invoiceId), Set.of(documentId), VisitType.OUTPATIENT);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAcceptsClaimWithoutPrescription() throws Exception {
        when(claimService.create(any(ClaimRequest.class))).thenReturn(sampleClaim());
        ClaimRequest withoutPrescription = new ClaimRequest(policyId, benefitId, null, null, visitorId,
                new BigDecimal("50000.00"), "Hospital stay", null,
                null, null, null, null, null);

        mockMvc.perform(post("/api/v1/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withoutPrescription)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreatedWithAugmentedFields() throws Exception {
        when(claimService.create(any(ClaimRequest.class))).thenReturn(sampleClaim());

        mockMvc.perform(post("/api/v1/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.insurerId").value(insurerId.toString()))
                .andExpect(jsonPath("$.prescription").value("Paracetamol 500mg twice daily"))
                .andExpect(jsonPath("$.invoices").isArray())
                .andExpect(jsonPath("$.invoiceIds").doesNotExist())
                .andExpect(jsonPath("$.documentIds[0]").value(documentId.toString()))
                .andExpect(jsonPath("$.claimedAmountBase").value(385.00))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsClaim() throws Exception {
        when(claimService.getById(claimId)).thenReturn(sampleClaim());

        mockMvc.perform(get("/api/v1/claims/{id}", claimId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(claimId.toString()))
                .andExpect(jsonPath("$.prescription").value("Paracetamol 500mg twice daily"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listReturnsClaims() throws Exception {
        when(claimService.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleClaim()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].visitorId").value(visitorId.toString()));

        verify(claimService).search(eq(ClaimSearchCriteria.none()),
                argThat(pageable -> pageable.getSort().equals(Sort.by(Sort.Direction.DESC, "createdDate"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listBindsEveryFilterFromQueryParams() throws Exception {
        UUID benefitA = UUID.randomUUID();
        UUID benefitB = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        UUID insurer = UUID.randomUUID();
        when(claimService.search(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/claims")
                        .param("status", "UNDER_REVIEW")
                        .param("q", "INV-77")
                        .param("dateFrom", "2026-10-01")
                        .param("dateTo", "2026-10-31")
                        .param("benefitIds", benefitA.toString(), benefitB.toString())
                        .param("serviceProviderIds", providerId.toString())
                        .param("providerUnlinked", "true")
                        .param("insurerIds", insurer.toString())
                        .param("minAmountBase", "10.5")
                        .param("maxAmountBase", "500")
                        .param("sentToMinet", "false")
                        .param("flags", "NO_PREAUTH,OUTSIDE_STAY")
                        .param("sort", "claimedAmountBase,asc"))
                .andExpect(status().isOk());

        verify(claimService).search(
                eq(new ClaimSearchCriteria(ClaimStatus.UNDER_REVIEW, "INV-77", LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31), List.of(benefitA, benefitB), List.of(providerId), true,
                        List.of(insurer), new BigDecimal("10.5"), new BigDecimal("500"), false,
                        List.of(ClaimAttentionFlag.NO_PREAUTH, ClaimAttentionFlag.OUTSIDE_STAY))),
                argThat(pageable -> pageable.getSort().equals(Sort.by(Sort.Direction.ASC, "claimedAmountBase"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRejectsUnknownFlag() throws Exception {
        mockMvc.perform(get("/api/v1/claims").param("flags", "LATE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void summaryReturnsCountsAndKpis() throws Exception {
        Map<ClaimStatus, Long> counts = new EnumMap<>(ClaimStatus.class);
        for (ClaimStatus status : ClaimStatus.values()) {
            counts.put(status, 0L);
        }
        counts.put(ClaimStatus.SUBMITTED, 3L);
        when(claimService.summary(any())).thenReturn(
                new ClaimListSummary(3, counts, 3, new BigDecimal("120.50"), 1, 2));

        mockMvc.perform(get("/api/v1/claims/summary").param("status", "PAID").param("q", "A123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.countsByStatus.SUBMITTED").value(3))
                .andExpect(jsonPath("$.countsByStatus.PAID").value(0))
                .andExpect(jsonPath("$.awaitingDecision").value(3))
                .andExpect(jsonPath("$.totalValueBase").value(120.50))
                .andExpect(jsonPath("$.needsAttention").value(1))
                .andExpect(jsonPath("$.notSentToMinet").value(2));

        verify(claimService).summary(argThat(criteria -> criteria.status() == ClaimStatus.PAID
                && "A123".equals(criteria.q())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void decideAppliesDecision() throws Exception {
        when(claimService.decide(any(), any())).thenReturn(sampleClaim());

        mockMvc.perform(post("/api/v1/claims/{id}/decision", claimId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"approvedAmount\":40000.00,\"reason\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void attachInvoiceAttachesInvoiceToClaim() throws Exception {
        when(claimService.attachInvoice(any(), any())).thenReturn(sampleClaim());

        mockMvc.perform(put("/api/v1/claims/{id}/invoice", claimId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceId\":\"" + invoiceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(claimId.toString()))
                .andExpect(jsonPath("$.invoices").isArray())
                .andExpect(jsonPath("$.invoiceIds").doesNotExist());
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/claims/{id}", claimId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportExcelReturnsWorkbookAttachment() throws Exception {
        when(claimService.listForExport(ClaimStatus.APPROVED, insurerId,
                java.time.LocalDate.of(2026, 10, 1), java.time.LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of());
        when(claimExcelWriter.write(List.of())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/claims/export/excel")
                        .param("status", "APPROVED")
                        .param("insurerId", insurerId.toString())
                        .param("dateFrom", "2026-10-01")
                        .param("dateTo", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"claims.xlsx\""))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardSummaryReturnsTotals() throws Exception {
        when(claimService.dashboardSummary(ClaimStatus.APPROVED, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)))
                .thenReturn(new ClaimDashboardSummary(12, new BigDecimal("3456.78")));

        mockMvc.perform(get("/api/v1/claims/dashboard/summary")
                        .param("status", "APPROVED")
                        .param("dateFrom", "2026-10-01")
                        .param("dateTo", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClaims").value(12))
                .andExpect(jsonPath("$.totalApprovedAmountUsd").value(3456.78));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardDiagnosesReturnsDistribution() throws Exception {
        UUID diagnosisId = UUID.randomUUID();
        when(claimService.diagnosisDistribution(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(new DiagnosisDistribution(diagnosisId, "B54", "Malaria", 3,
                        new BigDecimal("60.00"))));

        mockMvc.perform(get("/api/v1/claims/dashboard/diagnoses")
                        .param("dateFrom", "2026-10-01")
                        .param("dateTo", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosisId").value(diagnosisId.toString()))
                .andExpect(jsonPath("$[0].code").value("B54"))
                .andExpect(jsonPath("$[0].claimCount").value(3))
                .andExpect(jsonPath("$[0].percentage").value(60.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardDiagnosesByCountryReturnsGroupedDistribution() throws Exception {
        when(claimService.diagnosisDistributionByCountry(null, null)).thenReturn(List.of(
                new CountryDiagnosisDistribution("USA", 4, List.of(
                        new DiagnosisDistribution(UUID.randomUUID(), "B54", "Malaria", 3,
                                new BigDecimal("75.00"))))));

        mockMvc.perform(get("/api/v1/claims/dashboard/diagnoses/by-country"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].country").value("USA"))
                .andExpect(jsonPath("$[0].totalDiagnoses").value(4))
                .andExpect(jsonPath("$[0].diagnoses[0].title").value("Malaria"))
                .andExpect(jsonPath("$[0].diagnoses[0].percentage").value(75.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardDiagnosesRejectsInvalidDate() throws Exception {
        mockMvc.perform(get("/api/v1/claims/dashboard/diagnoses").param("dateFrom", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardByInsurerReturnsDistribution() throws Exception {
        when(claimService.distributionByInsurer(null, null, null)).thenReturn(List.of(
                new InsurerClaimDistribution(insurerId, "Jubilee Insurance", 4, new BigDecimal("100.00")),
                new InsurerClaimDistribution(null, "Unknown", 1, new BigDecimal("5.00"))));

        mockMvc.perform(get("/api/v1/claims/dashboard/by-insurer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].insurerName").value("Jubilee Insurance"))
                .andExpect(jsonPath("$[0].claimCount").value(4))
                .andExpect(jsonPath("$[0].approvedAmountUsd").value(100.00))
                .andExpect(jsonPath("$[1].insurerId").doesNotExist())
                .andExpect(jsonPath("$[1].insurerName").value("Unknown"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardByServiceProviderReturnsDistribution() throws Exception {
        UUID providerId = UUID.randomUUID();
        when(claimService.distributionByServiceProvider(null, null, null, null)).thenReturn(List.of(
                new ServiceProviderClaimDistribution(providerId, "Aga Khan Hospital", 9, new BigDecimal("900.00"))));

        mockMvc.perform(get("/api/v1/claims/dashboard/by-service-provider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceProviderId").value(providerId.toString()))
                .andExpect(jsonPath("$[0].serviceProviderName").value("Aga Khan Hospital"))
                .andExpect(jsonPath("$[0].claimCount").value(9));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardByServiceProviderPassesLimit() throws Exception {
        when(claimService.distributionByServiceProvider(null, null, null, 10)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/claims/dashboard/by-service-provider").param("limit", "10"))
                .andExpect(status().isOk());
        verify(claimService).distributionByServiceProvider(null, null, null, 10);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardRejectsInvalidStatusAndDate() throws Exception {
        mockMvc.perform(get("/api/v1/claims/dashboard/summary").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/claims/dashboard/by-insurer").param("dateFrom", "not-a-date"))
                .andExpect(status().isBadRequest());
    }
}
