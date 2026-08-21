package com.travel.insurance.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.user.dto.UserRequest;
import com.travel.insurance.user.dto.UserResponse;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID userId = UUID.randomUUID();

    private UserResponse sampleResponse() {
        return new UserResponse(userId, "Jane", "Doe", "jane@acme.com", "0700000000", Role.INSURER_USER,
                UUID.randomUUID(), Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getByIdReturnsUser() throws Exception {
        when(userService.getById(userId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.role").value("INSURER_USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsCreated() throws Exception {
        when(userService.create(any(UserRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserRequest("Jane", "Doe", "jane@acme.com", "password123",
                                        "0700000000", Role.INSURER_USER, UUID.randomUUID()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.role").value("INSURER_USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRejectsMissingRole() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserRequest("Jane", "Doe", "jane@acme.com", "password123",
                                        "0700000000", null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listReturnsUsers() throws Exception {
        when(userService.list(any())).thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("INSURER_USER"));
    }

    @Test
    void getWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isUnauthorized());
    }
}
