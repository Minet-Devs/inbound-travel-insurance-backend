package com.travel.insurance.insurer;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.config.SecurityConfig;
import com.travel.insurance.policy.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InsurerController.class)
@Import(SecurityConfig.class)
class InsurerAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InsurerService insurerService;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void contactCenterCanListInsurers() throws Exception {
        when(insurerService.list(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/insurers").param("page", "0").param("size", "100"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void contactCenterCannotReadSingleInsurer() throws Exception {
        mockMvc.perform(get("/api/v1/insurers/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void insurerUserCannotListInsurers() throws Exception {
        mockMvc.perform(get("/api/v1/insurers"))
                .andExpect(status().isForbidden());
    }
}
