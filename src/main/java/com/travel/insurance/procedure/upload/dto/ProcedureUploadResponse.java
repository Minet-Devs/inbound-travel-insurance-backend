package com.travel.insurance.procedure.upload.dto;

import com.travel.insurance.procedure.upload.ProcedureUploadStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Traceable view of an upload from validation through completion.
 */
public record ProcedureUploadResponse(
        UUID uploadPublicId,
        String originalFilename,
        UUID departmentPublicId,
        ProcedureUploadStatus status,
        int totalRows,
        int validRows,
        int createdRows,
        int skippedRows,
        int failedRows,
        UUID uploadedBy,
        Instant uploadTime,
        Instant processingStartTime,
        Instant completionTime,
        String failureReason
) {
}
