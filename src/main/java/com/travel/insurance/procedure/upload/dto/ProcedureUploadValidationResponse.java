package com.travel.insurance.procedure.upload.dto;

import com.travel.insurance.procedure.upload.ProcedureUploadStatus;

import java.util.List;
import java.util.UUID;

/**
 * Result of stage one (validate). No procedures are created; the upload public id
 * is used to trigger the import stage.
 */
public record ProcedureUploadValidationResponse(
        UUID uploadPublicId,
        ProcedureUploadStatus status,
        int totalRows,
        int validRows,
        int existingDuplicates,
        int invalidRows,
        boolean readyForImport,
        List<ProcedureUploadRowResult> rows
) {
}
