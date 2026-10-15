package com.travel.insurance.document.storage;

import com.travel.insurance.config.DocumentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Filesystem backend for local development and tests; production uses {@link AzureBlobDocumentStorage}. */
@Component
@ConditionalOnProperty(prefix = "app.documents", name = "storage", havingValue = "local", matchIfMissing = true)
public class LocalFileDocumentStorage implements DocumentStorage {

    private final Path root;

    public LocalFileDocumentStorage(DocumentProperties properties) {
        this.root = Path.of(properties.getLocal().getPath()).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, InputStream content, long length, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store document " + key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read document " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete document " + key, e);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
