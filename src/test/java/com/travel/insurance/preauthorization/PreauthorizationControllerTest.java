package com.travel.insurance.preauthorization;

import com.travel.insurance.preauthorization.dto.ServiceProviderPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.PreauthorizationDashboardSummary;
import com.travel.insurance.preauthorization.dto.InsurerPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadRowStatus;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadRowResult;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import org.springframework.mock.web.MockMultipartFile;
import com.travel.insurance.preauthorization.upload.PreauthorizationUploadService;
import com.travel.insurance.common.domain.VisitType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreauthorizationController.class)
class PreauthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PreauthorizationService preauthorizationService;

    @MockBean
    private PreauthorizationUploadService preauthorizationUploadService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID preauthorizationId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID visitorId = UUID.randomUUID();
    private final UUID icd11CodeId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();
    private final UUID serviceProviderId = UUID.randomUUID();

    private PreauthorizationResponse sampleResponse(PreauthorizationStatus status) {
        return new PreauthorizationResponse(
                preauthorizationId, policyId, UUID.randomUUID(), visitorId, "Jane Traveler", "A1234567",
                icd11CodeId, "1A00", "Cholera", benefitId, "Medical Expenses",
                serviceProviderId, "Aga Khan Hospital", null, null,
                new BigDecimal("500.00"), null, "X-ray", null, status,
                Instant.now(), Instant.now(), null, null, List.of(), false, VisitType.OUTPATIENT, null, null,
                new BigDecimal("0.007752"), new BigDecimal("3.88"));
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void createReturnsCreated() throws Exception {
        when(preauthorizationService.create(any(PreauthorizationRequest.class)))
                .thenReturn(sampleResponse(PreauthorizationStatus.PENDING));

        PreauthorizationRequest request = new PreauthorizationRequest(
                policyId, visitorId, icd11CodeId, benefitId, serviceProviderId, null,
                new BigDecimal("500.00"), "X-ray", null, VisitType.OUTPATIENT);

        mockMvc.perform(post("/api/v1/preauthorizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.icd11CodeId").value(icd11CodeId.toString()))
                .andExpect(jsonPath("$.exchangeRate").value(0.007752))
                .andExpect(jsonPath("$.approvedAmountInDollars").value(3.88));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardEndpointsReturnSummaryAndDistributions() throws Exception {
        UUID insurerId = UUID.randomUUID();
        when(preauthorizationService.dashboardSummary(any(), any()))
                .thenReturn(new PreauthorizationDashboardSummary(10, 5, 2, 1));
        when(preauthorizationService.distributionByInsurer(any(), any())).thenReturn(List.of(
                new InsurerPreauthorizationDistribution(insurerId, "Jubilee", 4, new BigDecimal("2500.00"))));
        when(preauthorizationService.topServiceProviders(any(), any(), any())).thenReturn(List.of(
                new ServiceProviderPreauthorizationDistribution(serviceProviderId, "Aga Khan", 3,
                        new BigDecimal("900.00"))));

        mockMvc.perform(get("/api/v1/preauthorizations/dashboard/summary").param("dateFrom", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPreauthorizations").value(10))
                .andExpect(jsonPath("$.approved").value(5))
                .andExpect(jsonPath("$.rejected").value(2))
                .andExpect(jsonPath("$.partiallyApproved").value(1));
        mockMvc.perform(get("/api/v1/preauthorizations/dashboard/by-insurer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].insurerName").value("Jubilee"))
                .andExpect(jsonPath("$[0].preauthorizationCount").value(4))
                .andExpect(jsonPath("$[0].approvedAmount").value(2500.00));
        mockMvc.perform(get("/api/v1/preauthorizations/dashboard/top-service-providers").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceProviderName").value("Aga Khan"))
                .andExpect(jsonPath("$[0].preauthorizationCount").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadReturnsPerRowResults() throws Exception {
        UUID createdId = UUID.randomUUID();
        when(preauthorizationUploadService.upload(any())).thenReturn(new PreauthorizationUploadResponse(2, 1, 1, List.of(
                new PreauthorizationUploadRowResult(2, "A1", PreauthorizationUploadRowStatus.CREATED, createdId, null),
                new PreauthorizationUploadRowResult(3, "B2", PreauthorizationUploadRowStatus.FAILED, null,
                        "No visitor found for passport B2"))));

        mockMvc.perform(multipart("/api/v1/preauthorizations/upload")
                        .file(new MockMultipartFile("file", "approvals.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1}))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdRows").value(1))
                .andExpect(jsonPath("$.failedRows").value(1))
                .andExpect(jsonPath("$.rows[0].preauthorizationId").value(createdId.toString()))
                .andExpect(jsonPath("$.rows[1].status").value("FAILED"))
                .andExpect(jsonPath("$.rows[1].errorMessage").value("No visitor found for passport B2"));
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void createRejectsMissingVisitorId() throws Exception {
        PreauthorizationRequest request = new PreauthorizationRequest(
                policyId, null, icd11CodeId, benefitId, serviceProviderId, null,
                new BigDecimal("500.00"), "X-ray", null, VisitType.OUTPATIENT);

        mockMvc.perform(post("/api/v1/preauthorizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsPreauthorization() throws Exception {
        when(preauthorizationService.getById(preauthorizationId))
                .thenReturn(sampleResponse(PreauthorizationStatus.PENDING));

        mockMvc.perform(get("/api/v1/preauthorizations/{id}", preauthorizationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(preauthorizationId.toString()));
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void updateReturnsUpdatedPreauthorization() throws Exception {
        when(preauthorizationService.update(eq(preauthorizationId), any(PreauthorizationRequest.class)))
                .thenReturn(sampleResponse(PreauthorizationStatus.PENDING));

        PreauthorizationRequest request = new PreauthorizationRequest(
                policyId, visitorId, icd11CodeId, benefitId, serviceProviderId, null,
                new BigDecimal("500.00"), "X-ray", null, VisitType.OUTPATIENT);

        mockMvc.perform(put("/api/v1/preauthorizations/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(preauthorizationId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void updateRejectsMissingVisitorId() throws Exception {
        PreauthorizationRequest request = new PreauthorizationRequest(
                policyId, null, icd11CodeId, benefitId, serviceProviderId, null,
                new BigDecimal("500.00"), "X-ray", null, null);

        mockMvc.perform(put("/api/v1/preauthorizations/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void decideReturnsUpdatedPreauthorization() throws Exception {
        when(preauthorizationService.decide(eq(preauthorizationId), any(PreauthorizationDecisionRequest.class)))
                .thenReturn(sampleResponse(PreauthorizationStatus.APPROVED));

        PreauthorizationDecisionRequest request = new PreauthorizationDecisionRequest(
                PreauthorizationStatus.APPROVED, new BigDecimal("500.00"), "approved in full");

        mockMvc.perform(post("/api/v1/preauthorizations/{id}/decision", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/preauthorizations/{id}", preauthorizationId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listFiltersByInsurerId() throws Exception {
        UUID insurerId = UUID.randomUUID();
        when(preauthorizationService.list(eq(insurerId), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(PreauthorizationStatus.PENDING))));

        mockMvc.perform(get("/api/v1/preauthorizations").param("insurerId", insurerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(preauthorizationId.toString()));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/preauthorizations/{id}", preauthorizationId))
                .andExpect(status().isUnauthorized());
    }
}