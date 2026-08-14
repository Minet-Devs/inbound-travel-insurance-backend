package com.travel.insurance.icd11;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.icd11.dto.Icd11ImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Icd11CodeController.class)
class Icd11CodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Icd11CodeService icd11CodeService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "ADMIN")
    void importReturnsResultSummary() throws Exception {
        when(icd11CodeService.importFromExcel(any())).thenReturn(new Icd11ImportResult(3, 2, 1, 0));
        MockMultipartFile file = new MockMultipartFile("file", "codes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/icd11-codes/import").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.inserted").value(2))
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    @Test
    @WithMockUser
    void searchReturnsPageOfCodes() throws Exception {
        Page<Icd11CodeResponse> page = new PageImpl<>(List.of(new Icd11CodeResponse(
                UUID.randomUUID(), "1A00", "Cholera", Instant.now(), Instant.now())));
        when(icd11CodeService.search(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/icd11-codes").param("query", "chol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("1A00"))
                .andExpect(jsonPath("$.content[0].title").value("Cholera"));
    }

    @Test
    @WithMockUser
    void searchByTitleReturnsMatches() throws Exception {
        Page<Icd11CodeResponse> page = new PageImpl<>(List.of(new Icd11CodeResponse(
                UUID.randomUUID(), "1A07", "Salmonella infection", Instant.now(), Instant.now())));
        when(icd11CodeService.searchByTitle(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/icd11-codes/search").param("title", "Salmonella"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("1A07"))
                .andExpect(jsonPath("$.content[0].title").value("Salmonella infection"));
    }

    @Test
    @WithMockUser
    void getByCodeReturnsCode() throws Exception {
        when(icd11CodeService.getByCode("1A00")).thenReturn(new Icd11CodeResponse(
                UUID.randomUUID(), "1A00", "Cholera", Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/icd11-codes/{code}", "1A00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1A00"))
                .andExpect(jsonPath("$.title").value("Cholera"));
    }

    @Test
    @WithMockUser
    void getByIdReturnsCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(icd11CodeService.getById(id)).thenReturn(new Icd11CodeResponse(
                id, "1A00", "Cholera", Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/icd11-codes/by-id/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1A00"))
                .andExpect(jsonPath("$.title").value("Cholera"));
    }

    @Test
    void importWithoutAuthenticationIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "codes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/icd11-codes/import").file(file).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
