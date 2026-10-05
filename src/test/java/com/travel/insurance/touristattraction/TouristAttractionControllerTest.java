package com.travel.insurance.touristattraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.touristattraction.dto.TouristAttractionRequest;
import com.travel.insurance.touristattraction.dto.TouristAttractionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TouristAttractionController.class)
class TouristAttractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TouristAttractionService touristAttractionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID attractionId = UUID.randomUUID();

    private TouristAttractionResponse sampleResponse() {
        return new TouristAttractionResponse(attractionId, "Lake Nakuru National Park", "Nakuru",
                Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser
    void getByIdReturnsAttraction() throws Exception {
        when(touristAttractionService.getById(attractionId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/tourist-attractions/{id}", attractionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attractionId.toString()))
                .andExpect(jsonPath("$.name").value("Lake Nakuru National Park"))
                .andExpect(jsonPath("$.county").value("Nakuru"));
    }

    @Test
    @WithMockUser
    void listReturnsPage() throws Exception {
        when(touristAttractionService.list(any())).thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/tourist-attractions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Lake Nakuru National Park"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreated() throws Exception {
        when(touristAttractionService.create(any(TouristAttractionRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/tourist-attractions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TouristAttractionRequest("Lake Nakuru National Park", "Nakuru"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lake Nakuru National Park"))
                .andExpect(jsonPath("$.county").value("Nakuru"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsMissingNameOrCounty() throws Exception {
        mockMvc.perform(post("/api/v1/tourist-attractions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TouristAttractionRequest("", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tourist-attractions/{id}", attractionId))
                .andExpect(status().isUnauthorized());
    }
}
