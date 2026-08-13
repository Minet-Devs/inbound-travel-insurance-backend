package com.travel.insurance.procedure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.procedure.dto.ProcedureRequest;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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

@WebMvcTest(ProcedureController.class)
class ProcedureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcedureService procedureService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private ProcedureResponse sampleResponse(UUID id, String name) {
        return new ProcedureResponse(id, "PRC-0001", name, "desc", UUID.randomUUID(),
                true,  null, Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser
    void createReturnsCreatedProcedure() throws Exception {
        UUID id = UUID.randomUUID();
        when(procedureService.create(any())).thenReturn(sampleResponse(id, "Nebulization"));
        ProcedureRequest request = new ProcedureRequest("Nebulization", "desc", UUID.randomUUID());

        mockMvc.perform(post("/api/v1/procedures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.procedureCode").value("PRC-0001"))
                .andExpect(jsonPath("$.name").value("Nebulization"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser
    void createRejectsMissingName() throws Exception {
        ProcedureRequest request = new ProcedureRequest("  ", "desc", UUID.randomUUID());

        mockMvc.perform(post("/api/v1/procedures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getByIdReturnsProcedure() throws Exception {
        UUID id = UUID.randomUUID();
        when(procedureService.getById(id)).thenReturn(sampleResponse(id, "Nebulization"));

        mockMvc.perform(get("/api/v1/procedures/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nebulization"));
    }

    @Test
    @WithMockUser
    void listReturnsPage() throws Exception {
        Page<ProcedureResponse> page = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID(), "Nebulization")));
        when(procedureService.list(eq("neb"), any(), eq(true), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/procedures")
                        .param("search", "neb").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Nebulization"));
    }

    @Test
    @WithMockUser
    void deactivateReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        ProcedureResponse inactive = new ProcedureResponse(id, "PRC-0001", "Nebulization", "desc",
                UUID.randomUUID(), false, null, Instant.now(), Instant.now());
        when(procedureService.deactivate(id)).thenReturn(inactive);

        mockMvc.perform(patch("/api/v1/procedures/{id}/deactivate", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void createWithoutAuthenticationIsRejected() throws Exception {
        ProcedureRequest request = new ProcedureRequest("Nebulization", "desc", UUID.randomUUID());

        mockMvc.perform(post("/api/v1/procedures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
