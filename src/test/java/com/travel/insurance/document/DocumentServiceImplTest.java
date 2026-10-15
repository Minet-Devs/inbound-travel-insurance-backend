package com.travel.insurance.document;

import com.travel.insurance.claim.Claim;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.claim.ClaimStatus;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.util.AuthenticatedUser;
import com.travel.insurance.config.DocumentProperties;
import com.travel.insurance.document.dto.DocumentResponse;
import com.travel.insurance.document.storage.DocumentStorage;
import com.travel.insurance.preauthorization.Preauthorization;
import com.travel.insurance.preauthorization.PreauthorizationService;
import com.travel.insurance.preauthorization.PreauthorizationStatus;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private ClaimService claimService;
    @Mock
    private PreauthorizationService preauthorizationService;
    @Mock
    private ServiceProviderService serviceProviderService;

    private DocumentServiceImpl documentService;

    private final UUID claimId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(documentRepository, documentStorage,
                new PdfMergeService(new DocumentProperties()), claimService, preauthorizationService,
                serviceProviderService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, organizationId, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Claim claim(ClaimStatus status) {
        Claim claim = new Claim();
        claim.setStatus(status);
        claim.setServiceProviderId(providerId);
        return claim;
    }

    private List<MultipartFile> twoPdfs() {
        return List.of(
                PdfFixtures.pdfPart("form.pdf", PdfFixtures.pdf(1, 300)),
                PdfFixtures.pdfPart("invoice.pdf", PdfFixtures.pdf(2, 400)));
    }

    private Document storedDocument(UUID uploadedBy) {
        Document document = new Document();
        document.setOwnerType(DocumentOwnerType.CLAIM);
        document.setOwnerId(claimId);
        document.setDocumentType(DocumentType.INVOICE);
        document.setFilename("x.pdf");
        document.setContentType("application/pdf");
        document.setStorageKey("claim/" + claimId + "/doc");
        document.setUploadedBy(uploadedBy);
        return document;
    }

    @Test
    void uploadMergesStoresBlobAndSavesOneDocument() {
        authenticateAs("ADMIN");
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.OPEN));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = documentService.upload(
                DocumentOwnerType.CLAIM, claimId, DocumentType.INVOICE, "Claim 42 / pack", twoPdfs());

        assertThat(response.pageCount()).isEqualTo(3);
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.filename()).isEqualTo("Claim 42   pack.pdf");
        assertThat(response.sourceFiles()).hasSize(2);
        ArgumentCaptor<Document> saved = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(saved.capture());
        assertThat(saved.getValue().getUploadedBy()).isEqualTo(userId);
        assertThat(saved.getValue().getStorageKey()).startsWith("claim/" + claimId + "/");
        verify(documentStorage).put(eq(saved.getValue().getStorageKey()), any(InputStream.class),
                eq(saved.getValue().getSizeBytes()), eq("application/pdf"));
    }

    @Test
    void uploadGeneratesFilenameWhenNoTitle() {
        authenticateAs("ADMIN");
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.SUBMITTED));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = documentService.upload(
                DocumentOwnerType.CLAIM, claimId, DocumentType.CLAIM_FORM, null, twoPdfs());

        assertThat(response.filename()).startsWith("documents-").endsWith(".pdf");
    }

    @Test
    void uploadDeletesBlobWhenDatabaseSaveFails() {
        authenticateAs("ADMIN");
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.OPEN));
        when(documentRepository.save(any(Document.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> documentService.upload(
                DocumentOwnerType.CLAIM, claimId, DocumentType.INVOICE, null, twoPdfs()))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(documentStorage).put(key.capture(), any(InputStream.class), anyLong(), anyString());
        verify(documentStorage).delete(key.getValue());
    }

    @Test
    void uploadRejectsWholeBatchAndStoresNothingWhenOneFileIsNotAPdf() {
        authenticateAs("ADMIN");
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.OPEN));
        MockMultipartFile image = new MockMultipartFile("files", "scan.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> documentService.upload(DocumentOwnerType.CLAIM, claimId, DocumentType.INVOICE,
                null, List.of(PdfFixtures.pdfPart("a.pdf", PdfFixtures.pdf(1, 300)), image)))
                .isInstanceOf(UnsupportedDocumentTypeException.class);

        verifyNoInteractions(documentStorage);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadIsBlockedOnceClaimIsDecided() {
        authenticateAs("ADMIN");
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.PAID));

        assertThatThrownBy(() -> documentService.upload(
                DocumentOwnerType.CLAIM, claimId, DocumentType.INVOICE, null, twoPdfs()))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(documentStorage);
    }

    @Test
    void uploadForPreauthorizationAllowedOnlyWhilePending() {
        authenticateAs("ADMIN");
        UUID preauthId = UUID.randomUUID();
        Preauthorization preauth = new Preauthorization();
        preauth.setServiceProviderId(providerId);
        preauth.setStatus(PreauthorizationStatus.APPROVED);
        when(preauthorizationService.getEntityById(preauthId)).thenReturn(preauth);

        assertThatThrownBy(() -> documentService.upload(
                DocumentOwnerType.PREAUTHORIZATION, preauthId, DocumentType.MEDICAL_REPORT, null, twoPdfs()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void providerUserCannotUploadToAnotherProvidersClaim() {
        authenticateAs("PROVIDER_USER");
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.OPEN));
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> documentService.upload(
                DocumentOwnerType.CLAIM, claimId, DocumentType.INVOICE, null, twoPdfs()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(documentStorage);
    }

    @Test
    void providerUserCanUploadToOwnClaim() {
        authenticateAs("PROVIDER_USER");
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.OPEN));
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(providerId));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = documentService.upload(
                DocumentOwnerType.CLAIM, claimId, DocumentType.INVOICE, null, twoPdfs());

        assertThat(response.pageCount()).isEqualTo(3);
    }

    @Test
    void listIsScopedToProviderToo() {
        authenticateAs("PROVIDER_USER");
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.OPEN));
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.list(DocumentOwnerType.CLAIM, claimId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void downloadStreamsStoredBytesAfterAccessCheck() {
        authenticateAs("ADMIN");
        Document document = storedDocument(userId);
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.PAID));
        when(documentStorage.get(document.getStorageKey())).thenReturn(new ByteArrayInputStream(new byte[]{1}));

        DocumentContent content = documentService.download(id);

        assertThat(content.metadata().filename()).isEqualTo("x.pdf");
        assertThat(content.stream()).isNotNull();
    }

    @Test
    void downloadOfUnknownDocumentIsNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.download(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void uploaderCanDeleteOwnDocumentWhileClaimIsOpen() {
        authenticateAs("PROVIDER_USER");
        Document document = storedDocument(userId);
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.OPEN));
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(providerId));

        documentService.delete(id);

        verify(documentRepository).delete(document);
        verify(documentStorage).delete(document.getStorageKey());
    }

    @Test
    void nonUploaderCannotDeleteButAdminCan() {
        Document document = storedDocument(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.OPEN));
        when(serviceProviderService.findIdByOrganizationId(organizationId)).thenReturn(Optional.of(providerId));

        authenticateAs("PROVIDER_USER");
        assertThatThrownBy(() -> documentService.delete(id)).isInstanceOf(AccessDeniedException.class);
        verify(documentRepository, never()).delete(any());

        authenticateAs("ADMIN");
        documentService.delete(id);
        verify(documentRepository).delete(document);
    }

    @Test
    void deleteIsBlockedOnceClaimIsPaid() {
        authenticateAs("ADMIN");
        Document document = storedDocument(userId);
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(claimService.getEntityById(claimId)).thenReturn(claim(ClaimStatus.PAID));

        assertThatThrownBy(() -> documentService.delete(id)).isInstanceOf(IllegalStateException.class);

        verify(documentRepository, never()).delete(any());
        verify(documentStorage, never()).delete(anyString());
    }
}
