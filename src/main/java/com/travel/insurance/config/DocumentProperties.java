package com.travel.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits and storage settings for claim / pre-authorization document uploads.
 * Bound from {@code app.documents.*} (see application.yml).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.documents")
public class DocumentProperties {

    /** Storage backend: {@code azure} or {@code local}. */
    private String storage = "local";

    /** Maximum number of PDFs accepted in one upload. */
    private int maxFiles = 10;

    /** Maximum size of a single uploaded PDF, in bytes. */
    private long maxFileSizeBytes = 10L * 1024 * 1024;

    /** Maximum combined size of all PDFs in one upload, in bytes. */
    private long maxTotalSizeBytes = 25L * 1024 * 1024;

    /** Maximum number of pages in the merged PDF. */
    private int maxTotalPages = 100;

    private final Azure azure = new Azure();

    private final Local local = new Local();

    @Getter
    @Setter
    public static class Azure {

        /** Storage account connection string; supplied through the environment, never committed. */
        private String connectionString;

        /** Private blob container holding the merged PDFs. */
        private String container = "claim-documents";
    }

    @Getter
    @Setter
    public static class Local {

        /** Root directory for the local filesystem backend (dev and tests). */
        private String path = "./data/documents";
    }
}
