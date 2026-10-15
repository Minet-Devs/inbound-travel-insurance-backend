package com.travel.insurance.document;

import com.travel.insurance.document.dto.SourceFileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** The result of a merge: a temp-file PDF plus its metadata. Closing it deletes the temp files. */
public record MergedPdf(
        Path file,
        Path workDir,
        int pageCount,
        long sizeBytes,
        String sha256,
        List<SourceFileInfo> sources) implements AutoCloseable {

    @Override
    public void close() {
        try (Stream<Path> paths = Files.walk(workDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // best effort; the OS temp cleaner will eventually remove leftovers
        }
    }
}
