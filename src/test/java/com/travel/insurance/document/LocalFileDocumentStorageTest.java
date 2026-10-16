package com.travel.insurance.document;

import com.travel.insurance.config.DocumentProperties;
import com.travel.insurance.document.storage.LocalFileDocumentStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileDocumentStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileDocumentStorage storage(Path root) {
        DocumentProperties properties = new DocumentProperties();
        properties.getLocal().setPath(root.toString());
        return new LocalFileDocumentStorage(properties);
    }

    @Test
    void putThenGetRoundTrips() throws Exception {
        LocalFileDocumentStorage storage = storage(tempDir);
        storage.put("claim/1/2", new ByteArrayInputStream("pdf".getBytes()), 3, "application/pdf");

        try (InputStream in = storage.get("claim/1/2")) {
            assertThat(in.readAllBytes()).isEqualTo("pdf".getBytes());
        }
    }

    @Test
    void ioFailureIsWrappedInDocumentStorageException() throws Exception {
        Path blocker = Files.createFile(tempDir.resolve("not-a-directory"));
        LocalFileDocumentStorage storage = storage(blocker);

        assertThatThrownBy(() -> storage.put("claim/1/2", new ByteArrayInputStream(new byte[]{1}), 1, "application/pdf"))
                .isInstanceOf(DocumentStorageException.class);
        assertThatThrownBy(() -> storage.get("claim/1/missing"))
                .isInstanceOf(DocumentStorageException.class);
    }
}
