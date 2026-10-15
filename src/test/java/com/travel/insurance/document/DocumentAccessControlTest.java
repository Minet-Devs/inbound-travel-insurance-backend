package com.travel.insurance.document;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Role matrix for document endpoints; imports {@link SecurityConfig} so {@code @PreAuthorize} is enforced. */
@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID claimId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    private int upload() throws Exception {
        return mockMvc.perform(multipart("/api/v1/claims/{id}/documents", claimId)
                        .file(new MockMultipartFile("files", "a.pdf", "application/pdf", new byte[]{1}))
                        .param("documentType", "INVOICE").with(csrf()))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpload() throws Exception {
        org.assertj.core.api.Assertions.assertThat(upload()).isEqualTo(201);
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void providerUserCanUpload() throws Exception {
        org.assertj.core.api.Assertions.assertThat(upload()).isEqualTo(201);
    }

    @Test
    @WithMockUser(roles = "CONTACT_CENTER")
    void contactCenterCanUpload() throws Exception {
        org.assertj.core.api.Assertions.assertThat(upload()).isEqualTo(201);
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void insurerUserCannotUploadOrDelete() throws Exception {
        org.assertj.core.api.Assertions.assertThat(upload()).isEqualTo(403);
        mockMvc.perform(delete("/api/v1/documents/{id}", documentId).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSURER_USER")
    void insurerUserCanListAndDownload() throws Exception {
        mockMvc.perform(get("/api/v1/claims/{id}/documents", claimId)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROVIDER_USER")
    void providerUserCanDeleteSubjectToServiceChecks() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{id}", documentId).with(csrf()))
                .andExpect(status().isNoContent());
    }
}
