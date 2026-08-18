package com.travel.insurance.memberstatement;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.memberstatement.dto.MemberStatementResponse;
import com.travel.insurance.memberstatement.dto.MemberStatementTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberStatementController.class)
class MemberStatementControllerTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberStatementService memberStatementService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private MemberStatementResponse sampleStatement() {
        return new MemberStatementResponse(
                UUID.randomUUID(), "Jane Traveler", "P1234567", UUID.randomUUID(), "POL-0001",
                List.of(),
                List.of(new MemberStatementTransaction(
                        UUID.randomUUID(), LocalDate.of(2026, 6, 1), UUID.randomUUID(), "Medical Expenses",
                        new BigDecimal("500.00"), UUID.randomUUID(), "Nairobi Hospital")));
    }

    @Test
    @WithMockUser
    void getStatementReturnsMemberDetails() throws Exception {
        when(memberStatementService.getStatement("P1234567")).thenReturn(sampleStatement());

        mockMvc.perform(get("/api/v1/member-statements").param("passportNumber", "P1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberName").value("Jane Traveler"))
                .andExpect(jsonPath("$.passportNumber").value("P1234567"))
                .andExpect(jsonPath("$.transactions[0].serviceProviderName").value("Nairobi Hospital"));
    }

    @Test
    void getStatementWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/member-statements").param("passportNumber", "P1234567"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void exportExcelReturnsXlsxAttachment() throws Exception {
        when(memberStatementService.export(eq("P1234567"), any(LocalDate.class), any(LocalDate.class),
                eq(MemberStatementExportType.EXCEL))).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/member-statements/export")
                        .param("passportNumber", "P1234567")
                        .param("fromDate", "2026-01-01")
                        .param("toDate", "2026-12-31")
                        .param("exportType", "EXCEL"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX))
                .andExpect(header().string("Content-Disposition",
                        containsString("member-statement-P1234567.xlsx")));
    }

    @Test
    @WithMockUser
    void exportPdfReturnsPdfAttachment() throws Exception {
        when(memberStatementService.export(eq("P1234567"), any(LocalDate.class), any(LocalDate.class),
                eq(MemberStatementExportType.PDF))).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/member-statements/export")
                        .param("passportNumber", "P1234567")
                        .param("fromDate", "2026-01-01")
                        .param("toDate", "2026-12-31")
                        .param("exportType", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        containsString("member-statement-P1234567.pdf")));
    }
}