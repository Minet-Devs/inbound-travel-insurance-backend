package com.travel.insurance.serviceprovider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceProviderController.class)
class ServiceProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceProviderService serviceProviderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID providerId = UUID.randomUUID();

    private ServiceProviderResponse sampleResponse() {
        return new ServiceProviderResponse(providerId, "Nairobi Hospital", "contact@nairobihospital.example",
                "+254700000000", "Argwings Kodhek Rd", Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsServiceProvider() throws Exception {
        when(serviceProviderService.getById(providerId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/service-providers/{id}", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(providerId.toString()))
                .andExpect(jsonPath("$.name").value("Nairobi Hospital"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreated() throws Exception {
        when(serviceProviderService.create(any(ServiceProviderRequest.class))).thenReturn(sampleResponse());

        ServiceProviderRequest request = new ServiceProviderRequest("Nairobi Hospital",
                "contact@nairobihospital.example", "+254700000000", "Argwings Kodhek Rd");
        mockMvc.perform(post("/api/v1/service-providers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nairobi Hospital"))
                .andExpect(jsonPath("$.contactEmail").value("contact@nairobihospital.example"))
                .andExpect(jsonPath("$.address").value("Argwings Kodhek Rd"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsInvalidBody() throws Exception {
        ServiceProviderRequest request = new ServiceProviderRequest("", "not-an-email", null, null);
        mockMvc.perform(post("/api/v1/service-providers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/service-providers/{id}", providerId))
                .andExpect(status().isUnauthorized());
    }
}
