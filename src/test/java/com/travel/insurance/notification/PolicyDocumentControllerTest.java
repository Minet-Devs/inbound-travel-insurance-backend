package com.travel.insurance.notification;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyDocumentController.class)
class PolicyDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitorActivatedNotificationListener documentGenerator;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID visitorId = UUID.randomUUID();

    @Test
    @WithMockUser
    void returnsMergedPdf() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        when(documentGenerator.generateCertificateAndReceiptPdf(visitorId)).thenReturn(pdf);

        mockMvc.perform(get("/api/v1/visitors/{id}/policy-documents", visitorId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser
    void returns404WhenVisitorUnknown() throws Exception {
        when(documentGenerator.generateCertificateAndReceiptPdf(visitorId))
                .thenThrow(new ResourceNotFoundException("Visitor", visitorId.toString()));

        mockMvc.perform(get("/api/v1/visitors/{id}/policy-documents", visitorId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void returns409WhenBenefitScheduleIncomplete() throws Exception {
        when(documentGenerator.generateCertificateAndReceiptPdf(visitorId))
                .thenThrow(new IllegalStateException("incomplete"));

        mockMvc.perform(get("/api/v1/visitors/{id}/policy-documents", visitorId))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void returnsPolicyWordingPdf() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        when(documentGenerator.generatePolicyWordingPdf(visitorId)).thenReturn(pdf);

        mockMvc.perform(get("/api/v1/visitors/{id}/policy-document", visitorId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser
    void policyWordingReturns404WhenVisitorUnknown() throws Exception {
        when(documentGenerator.generatePolicyWordingPdf(visitorId))
                .thenThrow(new ResourceNotFoundException("Visitor", visitorId.toString()));

        mockMvc.perform(get("/api/v1/visitors/{id}/policy-document", visitorId))
                .andExpect(status().isNotFound());
    }
}
