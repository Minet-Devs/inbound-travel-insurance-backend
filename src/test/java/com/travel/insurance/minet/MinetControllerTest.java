package com.travel.insurance.minet;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MinetController.class)
class MinetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinetVisitorSyncService minetVisitorSyncService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID visitorId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void memberByCertificateReturnsMinetMemberFields() throws Exception {
        when(minetVisitorSyncService.getMemberByCertificateNumber("ACME-2026-000123"))
                .thenReturn(new MinetVisitorRequest("A1234567", "Anna", "Smith", "Maria", "FEMALE",
                        java.time.LocalDate.of(1994, 7, 1), java.time.LocalDate.of(2026, 11, 6),
                        "a@example.com", "+254700000000", "IND"));

        mockMvc.perform(get("/api/v1/visitors/minet/certificate/ACME-2026-000123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberNumber").value("A1234567"))
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.dateOfBirth").value("1994-07-01"))
                .andExpect(jsonPath("$.dateJoined").value("2026-11-06"))
                .andExpect(jsonPath("$.residenceCountry").value("IND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void memberByUnknownCertificateReturns404() throws Exception {
        when(minetVisitorSyncService.getMemberByCertificateNumber("NOPE"))
                .thenThrow(new com.travel.insurance.common.exception.ResourceNotFoundException("Visitor", "NOPE"));

        mockMvc.perform(get("/api/v1/visitors/minet/certificate/NOPE")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncReturnsSentFlag() throws Exception {
        when(minetVisitorSyncService.syncByPassportNumber("A1234567"))
                .thenReturn(new MinetSyncResponse(visitorId, true));

        mockMvc.perform(post("/api/v1/visitors/minet-sync/A1234567").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorId").value(visitorId.toString()))
                .andExpect(jsonPath("$.sentToMinet").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unknownPassportReturns404() throws Exception {
        when(minetVisitorSyncService.syncByPassportNumber("X"))
                .thenThrow(new ResourceNotFoundException("Visitor", "X"));

        mockMvc.perform(post("/api/v1/visitors/minet-sync/X").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void visitorWithoutCertificateReturns409() throws Exception {
        when(minetVisitorSyncService.syncByPassportNumber("A1"))
                .thenThrow(new IllegalStateException("no certificate"));

        mockMvc.perform(post("/api/v1/visitors/minet-sync/A1").with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void minetFailureReturns502() throws Exception {
        when(minetVisitorSyncService.syncByPassportNumber("A1"))
                .thenThrow(new MinetClientException("status 500"));

        mockMvc.perform(post("/api/v1/visitors/minet-sync/A1").with(csrf()))
                .andExpect(status().isBadGateway());
    }
}
