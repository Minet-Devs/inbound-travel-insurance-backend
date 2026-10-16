package com.travel.insurance.document.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.travel.insurance.config.DocumentProperties;
import com.travel.insurance.document.DocumentStorageException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@ConditionalOnProperty(prefix = "app.documents", name = "storage", havingValue = "azure")
public class AzureBlobDocumentStorage implements DocumentStorage {

    private final BlobContainerClient container;

    public AzureBlobDocumentStorage(DocumentProperties properties) {
        String connectionString = properties.getAzure().getConnectionString();
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException(
                    "app.documents.azure.connection-string must be set when app.documents.storage=azure");
        }
        this.container = new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(properties.getAzure().getContainer())
                .buildClient();
        if (!this.container.exists()) {
            this.container.create();
        }
    }

    @Override
    public void put(String key, InputStream content, long length, String contentType) {
        try {
            BlobClient blob = container.getBlobClient(key);
            blob.upload(content, length, true);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
        } catch (RuntimeException e) {
            throw new DocumentStorageException("Could not store document " + key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return container.getBlobClient(key).openInputStream();
        } catch (RuntimeException e) {
            throw new DocumentStorageException("Could not read document " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            container.getBlobClient(key).delete();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() != 404) {
                throw new DocumentStorageException("Could not delete document " + key, e);
            }
        }
    }
}
