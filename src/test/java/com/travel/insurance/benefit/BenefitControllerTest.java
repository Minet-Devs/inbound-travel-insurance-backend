package com.travel.insurance.benefit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BenefitController.class)
class BenefitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BenefitService benefitService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID benefitId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreatedBenefit() throws Exception {
        when(benefitService.create(any())).thenReturn(new BenefitResponse(
                benefitId, "Medical Expenses", new BigDecimal("20000.00"), Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/v1/benefits").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BenefitRequest("Medical Expenses", new BigDecimal("20000.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.benefitName").value("Medical Expenses"))
                .andExpect(jsonPath("$.limitAmount").value(20000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsBenefit() throws Exception {
        when(benefitService.getById(benefitId)).thenReturn(new BenefitResponse(
                benefitId, "Mental Illness", new BigDecimal("1000.00"), Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/benefits/{id}", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.benefitName").value("Mental Illness"))
                .andExpect(jsonPath("$.limitAmount").value(1000.00));
    }

    @Test
    void createWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/benefits").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BenefitRequest("Medical Expenses", new BigDecimal("20000.00")))))
                .andExpect(status().isUnauthorized());
    }
}
