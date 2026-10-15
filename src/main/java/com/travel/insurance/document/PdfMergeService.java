package com.travel.insurance.document;

import com.travel.insurance.config.DocumentProperties;
import com.travel.insurance.document.dto.SourceFileInfo;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Validates uploaded PDFs and merges them, in request order, into a single PDF. Work happens in a
 * temp directory (PDFBox spills to disk) so large uploads do not sit in heap.
 */
@Component
@RequiredArgsConstructor
public class PdfMergeService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final DocumentProperties properties;

    /** Caller must close the result. Any invalid file rejects the whole upload and leaves nothing behind. */
    public MergedPdf merge(List<MultipartFile> files) {
        validateBatch(files);
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("claim-docs-");
            List<Path> parts = new ArrayList<>();
            List<SourceFileInfo> sources = new ArrayList<>();
            int totalPages = 0;
            long totalBytes = 0;
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                validateFile(file);
                totalBytes += file.getSize();
                if (totalBytes > properties.getMaxTotalSizeBytes()) {
                    throw new DocumentTooLargeException("Combined upload size exceeds %d bytes"
                            .formatted(properties.getMaxTotalSizeBytes()));
                }
                Path part = workDir.resolve("part-" + i + ".pdf");
                copyAndCheckMagic(file, part);
                int pages = readPageCount(part, file.getOriginalFilename());
                totalPages += pages;
                if (totalPages > properties.getMaxTotalPages()) {
                    throw new IllegalArgumentException(
                            "Merged document would exceed %d pages".formatted(properties.getMaxTotalPages()));
                }
                parts.add(part);
                sources.add(new SourceFileInfo(file.getOriginalFilename(), file.getSize(), pages));
            }
            Path merged = workDir.resolve("merged.pdf");
            mergeInto(parts, merged);
            return new MergedPdf(merged, workDir, totalPages, Files.size(merged), sha256(merged), List.copyOf(sources));
        } catch (IOException e) {
            cleanUp(workDir);
            throw new UncheckedIOException("Could not merge documents", e);
        } catch (RuntimeException e) {
            cleanUp(workDir);
            throw e;
        }
    }

    private void validateBatch(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one PDF file is required");
        }
        if (files.size() > properties.getMaxFiles()) {
            throw new IllegalArgumentException("At most %d files can be uploaded at once"
                    .formatted(properties.getMaxFiles()));
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded files must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !PDF_CONTENT_TYPE.equalsIgnoreCase(contentType.split(";")[0].trim())) {
            throw new UnsupportedDocumentTypeException(
                    "Only PDF files are accepted: " + file.getOriginalFilename());
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new DocumentTooLargeException("File %s exceeds %d bytes"
                    .formatted(file.getOriginalFilename(), properties.getMaxFileSizeBytes()));
        }
    }

    private void copyAndCheckMagic(MultipartFile file, Path target) throws IOException {
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        }
        byte[] head = new byte[PDF_MAGIC.length];
        try (InputStream in = Files.newInputStream(target)) {
            int read = in.readNBytes(head, 0, head.length);
            if (read < head.length || !Arrays.equals(head, PDF_MAGIC)) {
                throw new UnsupportedDocumentTypeException(
                        "File is not a PDF: " + file.getOriginalFilename());
            }
        }
    }

    private int readPageCount(Path part, String name) {
        try (PDDocument document = Loader.loadPDF(part.toFile())) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Encrypted PDFs are not accepted: " + name);
            }
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new IllegalArgumentException("Not a valid PDF (or password protected): " + name);
        }
    }

    private void mergeInto(List<Path> parts, Path target) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(target.toString());
        for (Path part : parts) {
            merger.addSource(part.toFile());
        }
        merger.mergeDocuments(IOUtils.createTempFileOnlyStreamCache());
    }

    private static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void cleanUp(Path workDir) {
        if (workDir != null) {
            new MergedPdf(null, workDir, 0, 0, null, List.of()).close();
        }
    }
}
