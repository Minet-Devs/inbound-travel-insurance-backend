package com.travel.insurance.benefit;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.benefit.dto.BenefitTypeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BenefitController.class)
class BenefitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BenefitService benefitService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listBenefitTypesReturnsFixedCatalog() throws Exception {
        when(benefitService.listBenefitTypes()).thenReturn(List.of(
                new BenefitTypeResponse(BenefitType.MEDICAL_EXPENSES, new BigDecimal("20000.00")),
                new BenefitTypeResponse(BenefitType.PRESCRIBED_MEDICINES, new BigDecimal("300.00"))));

        mockMvc.perform(get("/api/v1/benefits/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].benefitType").value("MEDICAL_EXPENSES"))
                .andExpect(jsonPath("$[0].fixedLimit").value(20000.00))
                .andExpect(jsonPath("$[1].benefitType").value("PRESCRIBED_MEDICINES"))
                .andExpect(jsonPath("$[1].fixedLimit").value(300.00));
    }

    @Test
    void listBenefitTypesWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/benefits/types"))
                .andExpect(status().isUnauthorized());
    }
}
