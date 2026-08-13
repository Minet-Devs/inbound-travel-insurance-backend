package com.travel.insurance.procedure.upload;

/**
 * Lifecycle of a procedure Excel upload, from acceptance through to import.
 */
public enum ProcedureUploadStatus {
    RECEIVED,
    VALIDATING,
    VALIDATION_FAILED,
    READY_FOR_IMPORT,
    PROCESSING,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED
}
