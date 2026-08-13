package com.travel.insurance.procedure.upload;

/**
 * Stable error codes for a failed or skipped upload row. The human-readable
 * message is built alongside and identifies the actual Excel row.
 */
public enum ProcedureUploadErrorCode {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    DEPARTMENT_REQUIRED,
    DEPARTMENT_NOT_FOUND,
    DUPLICATE_IN_FILE,
    ALREADY_EXISTS,
    INACTIVE_EXISTS,
    DB_CONFLICT
}
