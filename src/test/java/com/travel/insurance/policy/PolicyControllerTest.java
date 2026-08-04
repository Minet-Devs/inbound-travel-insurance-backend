package com.travel.insurance.policy;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.policy.dto.PolicyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1),
                PolicyStatus.ACTIVE, Instant.now(), Instant.now());
    }

    private BenefitResponse sampleBenefit() {
        return new BenefitResponse(benefitId, policyId, "Inpatient Cover",
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
                .andExpect(jsonPath("$.benefits[0].id").value(benefitId.toString()))
                .andExpect(jsonPath("$.benefits[0].name").value("Inpatient Cover"))
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
                .andExpect(jsonPath("$.content[0].benefits[0].name").value("Inpatient Cover"))
                .andExpect(jsonPath("$.content[0].benefits[0].limitAmount").value(100000.00));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isUnauthorized());
    }
}
