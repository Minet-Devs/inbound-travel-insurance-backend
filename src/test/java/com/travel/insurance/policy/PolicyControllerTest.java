package com.travel.insurance.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.BenefitType;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.policy.dto.PolicyRequest;
import com.travel.insurance.policy.dto.PolicyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private BenefitService benefitService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID policyId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();

    private PolicyResponse samplePolicy() {
        return new PolicyResponse(policyId, "POL-001", Set.of(UUID.randomUUID()),
                PolicyType.IPMI_61_DAYS_TO_12_MONTHS, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1),
                PolicyStatus.ACTIVE, Instant.now(), Instant.now());
    }

    private BenefitResponse sampleBenefit() {
        return new BenefitResponse(benefitId, policyId, BenefitType.EMERGENCY_MEDICAL_EXPENSES,
                new BigDecimal("100000.00"), Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsPolicyWithBenefits() throws Exception {
        when(policyService.getById(policyId)).thenReturn(samplePolicy());
        when(benefitService.listAllByPolicy(policyId)).thenReturn(List.of(sampleBenefit()));

        mockMvc.perform(get("/api/v1/policies/{id}", policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(policyId.toString()))
                .andExpect(jsonPath("$.policyNumber").value("POL-001"))
                .andExpect(jsonPath("$.policyType").value("IPMI_61_DAYS_TO_12_MONTHS"))
                .andExpect(jsonPath("$.benefits[0].id").value(benefitId.toString()))
                .andExpect(jsonPath("$.benefits[0].benefitType").value("EMERGENCY_MEDICAL_EXPENSES"))
                .andExpect(jsonPath("$.benefits[0].limitAmount").value(100000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listReturnsPoliciesWithBenefits() throws Exception {
        when(policyService.list(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(samplePolicy())));
        when(benefitService.listAllByPolicy(policyId)).thenReturn(List.of(sampleBenefit()));

        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(policyId.toString()))
                .andExpect(jsonPath("$.content[0].policyNumber").value("POL-001"))
                .andExpect(jsonPath("$.content[0].benefits[0].benefitType").value("EMERGENCY_MEDICAL_EXPENSES"))
                .andExpect(jsonPath("$.content[0].benefits[0].limitAmount").value(100000.00));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreated() throws Exception {
        when(policyService.create(any(PolicyRequest.class))).thenReturn(samplePolicy());

        PolicyRequest request = new PolicyRequest("POL-001", Set.of(UUID.randomUUID()),
                PolicyType.IPMI_61_DAYS_TO_12_MONTHS, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1), null);

        mockMvc.perform(post("/api/v1/policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyType").value("IPMI_61_DAYS_TO_12_MONTHS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsCoverPeriodOutsidePolicyType() throws Exception {
        when(policyService.create(any(PolicyRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Cover period of 90 day(s) is not valid for policy type SINGLE_ENTRY_UP_TO_30_DAYS "
                                + "(must be between 1 and 30 days)"));

        PolicyRequest request = new PolicyRequest("POL-001", Set.of(UUID.randomUUID()),
                PolicyType.SINGLE_ENTRY_UP_TO_30_DAYS, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1), null);

        mockMvc.perform(post("/api/v1/policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsMissingPolicyType() throws Exception {
        PolicyRequest request = new PolicyRequest("POL-001", Set.of(UUID.randomUUID()),
                null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1), null);

        mockMvc.perform(post("/api/v1/policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
