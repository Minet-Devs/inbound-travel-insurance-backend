package com.travel.insurance.document;

import com.travel.insurance.document.dto.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    /** Merges the PDFs, in the given order, into one PDF and stores it against the owner. */
    DocumentResponse upload(DocumentOwnerType ownerType, UUID ownerId, DocumentType documentType,
                            String title, List<MultipartFile> files);

    List<DocumentResponse> list(DocumentOwnerType ownerType, UUID ownerId);

    DocumentContent download(UUID id);

    void delete(UUID id);
}
