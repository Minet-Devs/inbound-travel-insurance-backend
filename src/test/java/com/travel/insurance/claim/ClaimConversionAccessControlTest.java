package com.travel.insurance.claim;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.common.domain.VisitType;
import com.travel.insurance.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role checks for converting a pre-authorization into a claim. Imports {@link SecurityConfig} so that
 * {@code @EnableMethodSecurity} is active and the controller's {@code @PreAuthorize} rules apply.
 */
@WebMvcTest(ClaimController.class)
@Import(SecurityConfig.class)
class ClaimConversionAccessControlTest {

    private static final String INVOICE = "{\"invoiceNumber\":\"INV-001\",\"issueDate\":\"2026-10-02\","
            + "\"invoiceItems\":[{\"description\":\"Consultation\",\"quantity\":1,\"unitPrice\":500,\"amount\":500}]}";
    private static final String BODY = "{\"invoices\":[" + INVOICE + "]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private ClaimExcelWriter claimExcelWriter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID preauthorizationId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void conversionPassesVisitTypeFromPayload() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("{\"invoices\"", "{\"visitType\":\"OUTPATIENT\",\"invoices\"")))
                .andExpect(status().isCreated());

        verify(claimService).createFromPreauthorization(eq(preauthorizationId),
                argThat(request -> request.visitType() == VisitType.OUTPATIENT));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void conversionRejectsUnknownVisitType() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("{\"invoices\"", "{\"visitType\":\"DAYCASE\",\"invoices\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanConvertPreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void contactCenterCanConvertPreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void providerUserCanConvertPreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void insurerUserCannotConvertPreauthorization() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void conversionAcceptsMultipleInvoices() throws Exception {
        String second = INVOICE.replace("INV-001", "INV-002");
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoices\":[" + INVOICE + "," + second + "]}"))
                .andExpect(status().isCreated());

        verify(claimService).createFromPreauthorization(eq(preauthorizationId),
                argThat(request -> request.invoices().size() == 2
                        && request.invoices().get(1).invoiceNumber().equals("INV-002")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void conversionRequiresAtLeastOneInvoice() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoices\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void conversionRequiresInvoiceItems() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoices\":[{\"invoiceNumber\":\"INV-002\",\"invoiceItems\":[]}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void conversionRequiresInvoiceNumber() throws Exception {
        mockMvc.perform(post("/api/v1/claims/from-preauthorization/{id}", preauthorizationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("INV-001", "  ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanExportClaimsExcel() throws Exception {
        mockMvc.perform(get("/api/v1/claims/export/excel"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void insurerUserCannotExportClaimsExcel() throws Exception {
        mockMvc.perform(get("/api/v1/claims/export/excel"))
                .andExpect(status().isForbidden());
    }
}
