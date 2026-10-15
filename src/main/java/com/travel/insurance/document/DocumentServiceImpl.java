package com.travel.insurance.document;

import com.travel.insurance.claim.Claim;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.claim.ClaimStatus;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.common.util.SecurityUtils;
import com.travel.insurance.document.dto.DocumentResponse;
import com.travel.insurance.document.storage.DocumentStorage;
import com.travel.insurance.preauthorization.Preauthorization;
import com.travel.insurance.preauthorization.PreauthorizationService;
import com.travel.insurance.preauthorization.PreauthorizationStatus;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final int MAX_FILENAME_LENGTH = 200;
    /** Placeholder until the row id is known; replaced inside the same transaction before commit. */
    private static final String PENDING_STORAGE_KEY = "pending";

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final PdfMergeService pdfMergeService;
    private final ClaimService claimService;
    private final PreauthorizationService preauthorizationService;
    private final ServiceProviderService serviceProviderService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public DocumentResponse upload(DocumentOwnerType ownerType, UUID ownerId, DocumentType documentType,
                                   String title, List<MultipartFile> files) {
        Owner owner = resolveOwner(ownerType, ownerId);
        requireMutable(owner);
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);

        try (MergedPdf merged = pdfMergeService.merge(files)) {
            Document document = new Document();
            document.setOwnerType(ownerType);
            document.setOwnerId(ownerId);
            document.setDocumentType(documentType);
            document.setFilename(buildFilename(title));
            document.setContentType(PDF_CONTENT_TYPE);
            document.setPageCount(merged.pageCount());
            document.setSizeBytes(merged.sizeBytes());
            document.setSha256(merged.sha256());
            document.setStorageKey(PENDING_STORAGE_KEY);
            document.setSourceFiles(merged.sources());
            if (user != null) {
                document.setUploadedBy(user.userId());
                document.setUploadedByOrganizationId(user.organizationId());
            }

            // The row id is generated on save, so insert first (inside a transaction, invisible to others),
            // then store the blob under that id. A failure at any later point rolls the row back.
            String[] storedKey = new String[1];
            Document saved;
            try {
                saved = transactionTemplate.execute(status -> {
                    Document inserted = documentRepository.saveAndFlush(document);
                    String storageKey = "%s/%s/%s".formatted(
                            ownerType.name().toLowerCase(), ownerId, inserted.getId());
                    storedKey[0] = storageKey;
                    try (InputStream in = Files.newInputStream(merged.file())) {
                        documentStorage.put(storageKey, in, merged.sizeBytes(), PDF_CONTENT_TYPE);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not read merged document", e);
                    }
                    inserted.setStorageKey(storageKey);
                    return inserted;
                });
            } catch (RuntimeException e) {
                if (storedKey[0] != null) {
                    deleteBlobQuietly(storedKey[0]);
                }
                throw e;
            }
            log.info("Document uploaded id={} owner={}:{} user={} pages={}", saved.getId(), ownerType, ownerId,
                    user != null ? user.userId() : null, saved.getPageCount());
            return toResponse(saved);
        }
    }

    @Override
    public List<DocumentResponse> list(DocumentOwnerType ownerType, UUID ownerId) {
        resolveOwner(ownerType, ownerId);
        return documentRepository.findAllByOwnerTypeAndOwnerIdOrderByCreatedDateAsc(ownerType, ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DocumentContent download(UUID id) {
        Document document = getEntity(id);
        resolveOwner(document.getOwnerType(), document.getOwnerId());
        log.info("Document downloaded id={} user={}", id, SecurityUtils.currentUserId().orElse(null));
        return new DocumentContent(toResponse(document), documentStorage.get(document.getStorageKey()));
    }

    @Override
    public void delete(UUID id) {
        Document document = getEntity(id);
        Owner owner = resolveOwner(document.getOwnerType(), document.getOwnerId());
        requireMutable(owner);
        if (!SecurityUtils.hasRole("ADMIN")) {
            UUID currentUserId = SecurityUtils.currentUserId().orElse(null);
            if (currentUserId == null || !currentUserId.equals(document.getUploadedBy())) {
                throw new AccessDeniedException("Only the uploader or an admin can delete this document");
            }
        }
        documentRepository.delete(document);
        deleteBlobQuietly(document.getStorageKey());
        log.info("Document deleted id={} owner={}:{} user={}", id, document.getOwnerType(), document.getOwnerId(),
                SecurityUtils.currentUserId().orElse(null));
    }

    private Document getEntity(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    /**
     * Loads the owning claim / pre-authorization and enforces provider scoping: a PROVIDER_USER can
     * only reach owners of their own service provider (reported as not found, so existence is not leaked).
     */
    private Owner resolveOwner(DocumentOwnerType type, UUID id) {
        Owner owner = switch (type) {
            case CLAIM -> {
                Claim claim = claimService.getEntityById(id);
                yield new Owner(type, id, claim.getServiceProviderId(),
                        claim.getStatus() == ClaimStatus.OPEN || claim.getStatus() == ClaimStatus.SUBMITTED);
            }
            case PREAUTHORIZATION -> {
                Preauthorization preauth = preauthorizationService.getEntityById(id);
                yield new Owner(type, id, preauth.getServiceProviderId(),
                        preauth.getStatus() == PreauthorizationStatus.PENDING);
            }
        };
        AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
        if (user != null && "PROVIDER_USER".equals(user.role())) {
            UUID providerId = serviceProviderService.findIdByOrganizationId(user.organizationId()).orElse(null);
            if (providerId == null || !providerId.equals(owner.serviceProviderId())) {
                throw new ResourceNotFoundException(type == DocumentOwnerType.CLAIM ? "Claim" : "Preauthorization", id);
            }
        }
        return owner;
    }

    private void requireMutable(Owner owner) {
        if (!owner.mutable()) {
            throw new IllegalStateException("Documents can no longer be changed for this %s in its current status"
                    .formatted(owner.type() == DocumentOwnerType.CLAIM ? "claim" : "pre-authorization"));
        }
    }

    private void deleteBlobQuietly(String storageKey) {
        try {
            documentStorage.delete(storageKey);
        } catch (RuntimeException e) {
            log.warn("Could not delete blob {}; it is orphaned and needs cleanup", storageKey, e);
        }
    }

    /** Title (or a generated name) reduced to a safe single-segment {@code .pdf} filename. */
    private static String buildFilename(String title) {
        String base = title == null ? "" : title.replaceAll("[\\p{Cntrl}/\\\\:*?\"<>|]", " ").trim();
        if (base.toLowerCase().endsWith(".pdf")) {
            base = base.substring(0, base.length() - 4).trim();
        }
        if (base.isEmpty()) {
            base = "documents-" + Instant.now().toEpochMilli();
        }
        if (base.length() > MAX_FILENAME_LENGTH) {
            base = base.substring(0, MAX_FILENAME_LENGTH).trim();
        }
        return base + ".pdf";
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOwnerType(),
                document.getOwnerId(),
                document.getDocumentType(),
                document.getFilename(),
                document.getContentType(),
                document.getPageCount(),
                document.getSizeBytes(),
                document.getSha256(),
                List.copyOf(document.getSourceFiles()),
                document.getUploadedBy(),
                document.getCreatedDate());
    }

    private record Owner(DocumentOwnerType type, UUID id, UUID serviceProviderId, boolean mutable) {
    }
}
