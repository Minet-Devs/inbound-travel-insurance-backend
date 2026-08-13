package com.travel.insurance.medicalservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.medicalservice.dto.MedicalServiceImportResult;
import com.travel.insurance.medicalservice.dto.MedicalServiceRequest;
import com.travel.insurance.medicalservice.dto.MedicalServiceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicalServiceController.class)
class MedicalServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MedicalServiceService medicalServiceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID serviceId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();

    private MedicalServiceResponse sampleResponse() {
        return new MedicalServiceResponse(
                serviceId, "LABORATORY GENERAL", departmentId, "LABORATORY", Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser
    void getByIdReturnsService() throws Exception {
        when(medicalServiceService.getById(serviceId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/medical-services/{id}", serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId.toString()))
                .andExpect(jsonPath("$.name").value("LABORATORY GENERAL"))
                .andExpect(jsonPath("$.departmentName").value("LABORATORY"));
    }

    @Test
    @WithMockUser
    void listReturnsPagedServicesFilteredByDepartment() throws Exception {
        Page<MedicalServiceResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(medicalServiceService.list(eq(departmentId), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/medical-services").param("departmentId", departmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("LABORATORY GENERAL"));
    }

    @Test
    @WithMockUser
    void listByDepartmentReturnsAllServices() throws Exception {
        when(medicalServiceService.listAllByDepartment(departmentId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/medical-services/by-department/{departmentId}", departmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].departmentId").value(departmentId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreated() throws Exception {
        when(medicalServiceService.create(any(MedicalServiceRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/medical-services")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MedicalServiceRequest("LABORATORY GENERAL", departmentId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("LABORATORY GENERAL"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void importReturnsResultSummary() throws Exception {
        when(medicalServiceService.importFromExcel(any()))
                .thenReturn(new MedicalServiceImportResult(9, 2, 7, 0));
        MockMultipartFile file = new MockMultipartFile("file", "services.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/medical-services/import").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(9))
                .andExpect(jsonPath("$.departmentsCreated").value(2))
                .andExpect(jsonPath("$.servicesInserted").value(7))
                .andExpect(jsonPath("$.servicesSkipped").value(0));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/medical-services/{id}", serviceId))
                .andExpect(status().isUnauthorized());
    }
}