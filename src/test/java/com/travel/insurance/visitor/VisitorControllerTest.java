package com.travel.insurance.visitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.benefit.BenefitType;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import com.travel.insurance.visitor.dto.VisitorStatusUpdate;
import com.travel.insurance.visitorbenefit.VisitorBenefitService;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VisitorController.class)
class VisitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VisitorService visitorService;

    @MockBean
    private VisitorBenefitService visitorBenefitService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID visitorId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();

    private VisitorResponse sampleVisitor() {
        return new VisitorResponse(visitorId, policyId, "Jane Traveler", "P1234567",
                LocalDate.of(1990, 5, 12), Gender.FEMALE, "Germany", "12 Example Street, Berlin",
                "jane.traveler@example.com", "+254700000000",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1),
                MaritalStatus.SINGLE, "Tourism", "https://storage.example.com/photos/jane.jpg", null,
                VisitorStatus.PENDING, "John Traveler", "+254711111111",
                Instant.now(), Instant.now());
    }

    private VisitorRequest sampleRequest() {
        return new VisitorRequest(policyId, "Jane Traveler", "P1234567",
                LocalDate.of(1990, 5, 12), Gender.FEMALE, "Germany", "12 Example Street, Berlin",
                "jane.traveler@example.com", "+254700000000",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1),
                MaritalStatus.SINGLE, "Tourism", "https://storage.example.com/photos/jane.jpg", null,
                "John Traveler", "+254711111111");
    }

    private VisitorBenefitResponse sampleBenefit() {
        return new VisitorBenefitResponse(UUID.randomUUID(), visitorId, benefitId,
                BenefitType.EMERGENCY_MEDICAL_EXPENSES, new BigDecimal("100000.00"), VisitorStatus.PENDING,
                Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsKycWithAssignedBenefits() throws Exception {
        when(visitorService.getById(visitorId)).thenReturn(sampleVisitor());
        when(visitorBenefitService.listAllByVisitor(visitorId))
                .thenReturn(List.of(sampleBenefit()));

        mockMvc.perform(get("/api/v1/visitors/{id}", visitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(visitorId.toString()))
                .andExpect(jsonPath("$.fullName").value("Jane Traveler"))
                .andExpect(jsonPath("$.passportNumber").value("P1234567"))
                .andExpect(jsonPath("$.policyId").value(policyId.toString()))
                .andExpect(jsonPath("$.visitorBenefits[0].benefitId").value(benefitId.toString()))
                .andExpect(jsonPath("$.visitorBenefits[0].benefitType").value("EMERGENCY_MEDICAL_EXPENSES"))
                .andExpect(jsonPath("$.visitorBenefits[0].limitAmount").value(100000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByPassportNumberReturnsKycWithAssignedBenefits() throws Exception {
        when(visitorService.getByPassportNumber("P1234567")).thenReturn(sampleVisitor());
        when(visitorBenefitService.listAllByVisitor(visitorId))
                .thenReturn(List.of(sampleBenefit()));

        mockMvc.perform(get("/api/v1/visitors/by-passport").param("passportNumber", "P1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passportNumber").value("P1234567"))
                .andExpect(jsonPath("$.visitorBenefits[0].benefitType").value("EMERGENCY_MEDICAL_EXPENSES"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByPolicyIdReturnsAllVisitorsWithAssignedBenefits() throws Exception {
        when(visitorService.listByPolicyId(policyId)).thenReturn(List.of(sampleVisitor()));
        when(visitorBenefitService.listAllByVisitor(visitorId))
                .thenReturn(List.of(sampleBenefit()));

        mockMvc.perform(get("/api/v1/visitors/by-policy").param("policyId", policyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyId").value(policyId.toString()))
                .andExpect(jsonPath("$[0].visitorBenefits[0].benefitType").value("EMERGENCY_MEDICAL_EXPENSES"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatusByPassportNumberReturnsUpdatedVisitor() throws Exception {
        when(visitorService.updateVisitorStatusByPassportNumber(
                eq("P1234567"), any(VisitorStatusUpdate.class)))
                .thenReturn(sampleVisitor());

        mockMvc.perform(patch("/api/v1/visitors/by-passport/status")
                        .with(csrf())
                        .param("passportNumber", "P1234567")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorStatus\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passportNumber").value("P1234567"));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/visitors/{id}", visitorId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreatedWithKycFields() throws Exception {
        when(visitorService.create(any(VisitorRequest.class))).thenReturn(sampleVisitor());

        mockMvc.perform(post("/api/v1/visitors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address").value("12 Example Street, Berlin"))
                .andExpect(jsonPath("$.reasonForTravel").value("Tourism"))
                .andExpect(jsonPath("$.facePhotoUrl").value("https://storage.example.com/photos/jane.jpg"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsMissingRequiredKycFields() throws Exception {
        VisitorRequest missingFields = new VisitorRequest(policyId, "Jane Traveler", "P1234567",
                LocalDate.of(1990, 5, 12), Gender.FEMALE, "Germany", "",
                "jane.traveler@example.com", "+254700000000",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1),
                MaritalStatus.SINGLE, "", "", null,
                "John Traveler", "+254711111111");

        mockMvc.perform(post("/api/v1/visitors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingFields)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
