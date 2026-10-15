package com.travel.insurance.document;

import com.travel.insurance.config.DocumentProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfMergeServiceTest {

    private DocumentProperties properties;
    private PdfMergeService pdfMergeService;

    @BeforeEach
    void setUp() {
        properties = new DocumentProperties();
        pdfMergeService = new PdfMergeService(properties);
    }

    @Test
    void mergesPdfsIntoOneWithPageCountEqualToSumOfParts() throws IOException {
        List<MultipartFile> files = List.of(
                PdfFixtures.pdfPart("form.pdf", PdfFixtures.pdf(2, 300)),
                PdfFixtures.pdfPart("invoice.pdf", PdfFixtures.pdf(3, 400)));

        try (MergedPdf merged = pdfMergeService.merge(files)) {
            assertThat(merged.pageCount()).isEqualTo(5);
            assertThat(merged.sizeBytes()).isEqualTo(Files.size(merged.file()));
            assertThat(merged.sha256()).hasSize(64);
            assertThat(merged.sources()).extracting("filename").containsExactly("form.pdf", "invoice.pdf");
            assertThat(merged.sources()).extracting("pageCount").containsExactly(2, 3);
            try (PDDocument document = Loader.loadPDF(merged.file().toFile())) {
                assertThat(document.getNumberOfPages()).isEqualTo(5);
            }
        }
    }

    @Test
    void keepsPagesInRequestOrder() throws IOException {
        List<MultipartFile> files = List.of(
                PdfFixtures.pdfPart("second.pdf", PdfFixtures.pdf(2, 500)),
                PdfFixtures.pdfPart("first.pdf", PdfFixtures.pdf(1, 300)));

        try (MergedPdf merged = pdfMergeService.merge(files);
             PDDocument document = Loader.loadPDF(merged.file().toFile())) {
            List<Float> widths = new java.util.ArrayList<>();
            document.getPages().forEach(page -> widths.add(page.getMediaBox().getWidth()));
            assertThat(widths).containsExactly(500f, 501f, 300f);
        }
    }

    @Test
    void closingTheResultDeletesTheTempFiles() {
        MergedPdf merged = pdfMergeService.merge(List.of(PdfFixtures.pdfPart("a.pdf", PdfFixtures.pdf(1, 300))));
        Path workDir = merged.workDir();
        assertThat(workDir).exists();

        merged.close();

        assertThat(workDir).doesNotExist();
    }

    @Test
    void rejectsImageContentType() {
        MockMultipartFile jpeg = new MockMultipartFile("files", "scan.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> pdfMergeService.merge(List.of(jpeg)))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
    }

    @Test
    void rejectsNonPdfBytesSentAsPdf() {
        MockMultipartFile fake = new MockMultipartFile("files", "fake.pdf", "application/pdf",
                "not really a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> pdfMergeService.merge(List.of(fake)))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
    }

    @Test
    void rejectsCorruptPdfAndRejectsWholeUpload() {
        MockMultipartFile corrupt = new MockMultipartFile("files", "bad.pdf", "application/pdf",
                "%PDF-1.4 garbage with no structure".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> pdfMergeService.merge(List.of(
                PdfFixtures.pdfPart("good.pdf", PdfFixtures.pdf(1, 300)), corrupt)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bad.pdf");
    }

    @Test
    void rejectsEncryptedPdf() {
        assertThatThrownBy(() -> pdfMergeService.merge(List.of(
                PdfFixtures.pdfPart("locked.pdf", PdfFixtures.encryptedPdf()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("locked.pdf");
    }

    @Test
    void rejectsEmptyFileList() {
        assertThatThrownBy(() -> pdfMergeService.merge(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooManyFiles() {
        properties.setMaxFiles(2);
        byte[] pdf = PdfFixtures.pdf(1, 300);

        assertThatThrownBy(() -> pdfMergeService.merge(List.of(
                PdfFixtures.pdfPart("a.pdf", pdf), PdfFixtures.pdfPart("b.pdf", pdf), PdfFixtures.pdfPart("c.pdf", pdf))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooManyPages() {
        properties.setMaxTotalPages(4);

        assertThatThrownBy(() -> pdfMergeService.merge(List.of(
                PdfFixtures.pdfPart("a.pdf", PdfFixtures.pdf(3, 300)),
                PdfFixtures.pdfPart("b.pdf", PdfFixtures.pdf(2, 300)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pages");
    }

    @Test
    void rejectsFileOverPerFileLimit() {
        properties.setMaxFileSizeBytes(100);

        assertThatThrownBy(() -> pdfMergeService.merge(List.of(
                PdfFixtures.pdfPart("big.pdf", PdfFixtures.pdf(1, 300)))))
                .isInstanceOf(DocumentTooLargeException.class);
    }

    @Test
    void rejectsCombinedSizeOverLimit() {
        byte[] pdf = PdfFixtures.pdf(1, 300);
        properties.setMaxTotalSizeBytes(pdf.length + 10L);

        assertThatThrownBy(() -> pdfMergeService.merge(List.of(
                PdfFixtures.pdfPart("a.pdf", pdf), PdfFixtures.pdfPart("b.pdf", pdf))))
                .isInstanceOf(DocumentTooLargeException.class);
    }
}
