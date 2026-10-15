package com.travel.insurance.document.dto;

import com.travel.insurance.document.DocumentOwnerType;
import com.travel.insurance.document.DocumentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        DocumentOwnerType ownerType,
        UUID ownerId,
        DocumentType documentType,
        String filename,
        String contentType,
        int pageCount,
        long sizeBytes,
        String sha256,
        List<SourceFileInfo> sourceFiles,
        UUID uploadedBy,
        Instant createdDate
) {
}
