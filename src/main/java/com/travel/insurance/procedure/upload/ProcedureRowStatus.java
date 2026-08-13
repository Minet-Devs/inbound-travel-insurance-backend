package com.travel.insurance.procedure.upload;

/**
 * Outcome of a single spreadsheet row. VALID rows are eligible for import;
 * CREATED/SKIPPED/FAILED are terminal import outcomes.
 */
public enum ProcedureRowStatus {
    VALID,
    CREATED,
    SKIPPED,
    FAILED
}
