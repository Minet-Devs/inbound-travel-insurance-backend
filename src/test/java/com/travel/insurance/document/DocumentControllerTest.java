package com.travel.insurance.document;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.document.dto.DocumentResponse;
import com.travel.insurance.document.dto.SourceFileInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID claimId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    private DocumentResponse response() {
        return new DocumentResponse(documentId, DocumentOwnerType.CLAIM, claimId, DocumentType.INVOICE,
                "pack.pdf", "application/pdf", 3, 1234, "a".repeat(64),
                List.of(new SourceFileInfo("a.pdf", 100, 1), new SourceFileInfo("b.pdf", 200, 2)),
                UUID.randomUUID(), Instant.parse("2026-10-15T10:00:00Z"));
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf", new byte[]{'%', 'P', 'D', 'F'});
    }

    @Test
    @WithMockUser
    void uploadAcceptsSeveralFilesAndReturnsOneDocument() throws Exception {
        when(documentService.upload(eq(DocumentOwnerType.CLAIM), eq(claimId), eq(DocumentType.INVOICE),
                eq("pack"), anyList())).thenReturn(response());

        mockMvc.perform(multipart("/api/v1/claims/{id}/documents", claimId)
                        .file(pdf("a.pdf")).file(pdf("b.pdf"))
                        .param("documentType", "INVOICE").param("title", "pack")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.pageCount").value(3))
                .andExpect(jsonPath("$.sourceFiles.length()").value(2));

        verify(documentService).upload(eq(DocumentOwnerType.CLAIM), eq(claimId), eq(DocumentType.INVOICE),
                eq("pack"), anyList());
    }

    @Test
    @WithMockUser
    void uploadWithoutFilesIsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/claims/{id}/documents", claimId)
                        .param("documentType", "INVOICE").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void uploadWithInvalidDocumentTypeIsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/claims/{id}/documents", claimId)
                        .file(pdf("a.pdf")).param("documentType", "SELFIE").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void uploadOfNonPdfIsUnsupportedMediaType() throws Exception {
        when(documentService.upload(any(), any(), any(), any(), anyList()))
                .thenThrow(new UnsupportedDocumentTypeException("Only PDF files are accepted: scan.jpg"));

        mockMvc.perform(multipart("/api/v1/claims/{id}/documents", claimId)
                        .file(pdf("scan.jpg")).param("documentType", "INVOICE").with(csrf()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser
    void uploadOverSizeLimitIsPayloadTooLarge() throws Exception {
        when(documentService.upload(any(), any(), any(), isNull(), anyList()))
                .thenThrow(new DocumentTooLargeException("too big"));

        mockMvc.perform(multipart("/api/v1/claims/{id}/documents", claimId)
                        .file(pdf("a.pdf")).param("documentType", "INVOICE").with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @WithMockUser
    void uploadToDecidedClaimIsConflict() throws Exception {
        when(documentService.upload(any(), any(), any(), any(), anyList()))
                .thenThrow(new IllegalStateException("Documents can no longer be changed"));

        mockMvc.perform(multipart("/api/v1/claims/{id}/documents", claimId)
                        .file(pdf("a.pdf")).param("documentType", "INVOICE").with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void uploadToPreauthorizationUsesPreauthorizationOwner() throws Exception {
        UUID preauthId = UUID.randomUUID();
        when(documentService.upload(eq(DocumentOwnerType.PREAUTHORIZATION), eq(preauthId),
                eq(DocumentType.MEDICAL_REPORT), isNull(), anyList())).thenReturn(response());

        mockMvc.perform(multipart("/api/v1/preauthorizations/{id}/documents", preauthId)
                        .file(pdf("a.pdf")).param("documentType", "MEDICAL_REPORT").with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void listReturnsDocuments() throws Exception {
        when(documentService.list(DocumentOwnerType.CLAIM, claimId)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/claims/{id}/documents", claimId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("pack.pdf"));
    }

    @Test
    @WithMockUser
    void downloadStreamsPdfWithSafeHeaders() throws Exception {
        byte[] bytes = new byte[]{'%', 'P', 'D', 'F'};
        DocumentResponse metadata = new DocumentResponse(documentId, DocumentOwnerType.CLAIM, claimId,
                DocumentType.INVOICE, "pack.pdf", "application/pdf", 1, bytes.length, "a".repeat(64),
                List.of(), null, null);
        when(documentService.download(documentId))
                .thenReturn(new DocumentContent(metadata, new ByteArrayInputStream(bytes)));

        mockMvc.perform(get("/api/v1/documents/{id}/content", documentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @WithMockUser
    void downloadOfUnknownDocumentIsNotFound() throws Exception {
        when(documentService.download(documentId)).thenThrow(new ResourceNotFoundException("Document", documentId));

        mockMvc.perform(get("/api/v1/documents/{id}/content", documentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{id}", documentId).with(csrf()))
                .andExpect(status().isNoContent());

        verify(documentService).delete(documentId);
    }

    @Test
    void listWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/claims/{id}/documents", claimId))
                .andExpect(status().is4xxClientError());
    }
}
