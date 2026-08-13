package com.travel.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operational limits for procedure Excel uploads, kept configurable rather than
 * hard-coded. Bound from {@code procedure.upload.*} (see application.yml).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "procedure.upload")
public class ProcedureUploadProperties {

    /** Maximum accepted upload size in bytes. */
    private long maxFileSizeBytes = 5L * 1024 * 1024;

    /** Maximum number of data rows accepted in one upload. */
    private int maxRows = 5000;

    /** Number of procedures persisted per batch during import. */
    private int batchSize = 100;

    /** Maximum length of a cleaned procedure name. */
    private int maxNameLength = 255;
}
