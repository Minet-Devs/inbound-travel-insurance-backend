package com.travel.insurance.document;

import com.travel.insurance.document.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /** PDFs only, in the order they should appear in the merged document. */
    @PostMapping(path = "/claims/{claimId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_USER', 'CONTACT_CENTER')")
    public ResponseEntity<DocumentResponse> uploadClaimDocuments(
            @PathVariable UUID claimId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) String title) {
        return upload(DocumentOwnerType.CLAIM, claimId, documentType, title, files);
    }

    @GetMapping("/claims/{claimId}/documents")
    public ResponseEntity<List<DocumentResponse>> listClaimDocuments(@PathVariable UUID claimId) {
        return ResponseEntity.ok(documentService.list(DocumentOwnerType.CLAIM, claimId));
    }

    @PostMapping(path = "/preauthorizations/{preauthorizationId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_USER', 'CONTACT_CENTER')")
    public ResponseEntity<DocumentResponse> uploadPreauthorizationDocuments(
            @PathVariable UUID preauthorizationId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) String title) {
        return upload(DocumentOwnerType.PREAUTHORIZATION, preauthorizationId, documentType, title, files);
    }

    @GetMapping("/preauthorizations/{preauthorizationId}/documents")
    public ResponseEntity<List<DocumentResponse>> listPreauthorizationDocuments(
            @PathVariable UUID preauthorizationId) {
        return ResponseEntity.ok(documentService.list(DocumentOwnerType.PREAUTHORIZATION, preauthorizationId));
    }

    /** Streamed through the API so the owner access check runs on every download. */
    @GetMapping("/documents/{id}/content")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        DocumentContent content = documentService.download(id);
        DocumentResponse metadata = content.metadata();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(metadata.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(metadata.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(content.stream()));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_USER', 'CONTACT_CENTER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<DocumentResponse> upload(DocumentOwnerType ownerType, UUID ownerId,
                                                    DocumentType documentType, String title,
                                                    List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(ownerType, ownerId, documentType, title, files));
    }
}
