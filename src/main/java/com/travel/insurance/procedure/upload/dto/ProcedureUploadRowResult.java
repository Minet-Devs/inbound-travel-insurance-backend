package com.travel.insurance.procedure.upload.dto;

import com.travel.insurance.procedure.upload.ProcedureRowStatus;

import java.util.UUID;

/**
 * Per-row outcome returned by validation and import. Identifies the actual Excel
 * row so clients can point the user at the exact line.
 */
public record ProcedureUploadRowResult(
        int excelRowNumber,
        String submittedName,
        String submittedDepartment,
        String submittedDescription,
        String cleanedName,
        ProcedureRowStatus status,
        String errorCode,
        String errorMessage,
        UUID createdProcedurePublicId
) {
}
