package com.travel.insurance.document.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.travel.insurance.config.DocumentProperties;
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
        BlobClient blob = container.getBlobClient(key);
        blob.upload(content, length, true);
        blob.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
    }

    @Override
    public InputStream get(String key) {
        return container.getBlobClient(key).openInputStream();
    }

    @Override
    public void delete(String key) {
        try {
            container.getBlobClient(key).delete();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() != 404) {
                throw e;
            }
        }
    }
}
