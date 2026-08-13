package com.travel.insurance.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import com.travel.insurance.invoice.dto.InvoiceItemResponse;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID invoiceId = UUID.randomUUID();
    private final UUID claimId = UUID.randomUUID();
    private final UUID medicalServiceId = UUID.randomUUID();

    private InvoiceResponse sampleInvoice() {
        return new InvoiceResponse(invoiceId, claimId, medicalServiceId, "In-patient Care",
                "INV-2026-001", LocalDate.of(2026, 8, 1), "KES", new BigDecimal("25000.00"),
                List.of(new InvoiceItemResponse(UUID.randomUUID(), "In-patient care",
                        new BigDecimal("1"), new BigDecimal("25000.00"),
                        new BigDecimal("25000.00"), LocalDate.of(2026, 8, 1))),
                Instant.now(), Instant.now());
    }

    private InvoiceRequest sampleRequest() {
        return new InvoiceRequest(claimId, medicalServiceId, "INV-2026-001",
                LocalDate.of(2026, 8, 1), "KES",
                new BigDecimal("25000.00"),
                List.of(new InvoiceItemRequest("In-patient care", new BigDecimal("1"),
                        new BigDecimal("25000.00"), new BigDecimal("25000.00"),
                        LocalDate.of(2026, 8, 1))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreatedWithLineItems() throws Exception {
        when(invoiceService.create(any(InvoiceRequest.class))).thenReturn(sampleInvoice());

        mockMvc.perform(post("/api/v1/invoices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-001"))
                .andExpect(jsonPath("$.claimId").value(claimId.toString()))
                .andExpect(jsonPath("$.medicalServiceId").value(medicalServiceId.toString()))
                .andExpect(jsonPath("$.medicalServiceName").value("In-patient Care"))
                .andExpect(jsonPath("$.invoiceItems[0].description").value("In-patient care"))
                .andExpect(jsonPath("$.invoiceItems[0].amount").value(25000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsInvoice() throws Exception {
        when(invoiceService.getById(invoiceId)).thenReturn(sampleInvoice());

        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-001"))
                .andExpect(jsonPath("$.totalAmount").value(25000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByClaimIdReturnsInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/invoices").param("claimId", claimId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsBlankLineItemDescription() throws Exception {
        InvoiceRequest invalid = new InvoiceRequest(claimId, null, "INV-2026-002",
                LocalDate.of(2026, 8, 2), "KES", new BigDecimal("1000.00"),
                List.of(new InvoiceItemRequest("", new BigDecimal("1"), new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"), LocalDate.of(2026, 8, 2))));

        mockMvc.perform(post("/api/v1/invoices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isUnauthorized());
    }
}
