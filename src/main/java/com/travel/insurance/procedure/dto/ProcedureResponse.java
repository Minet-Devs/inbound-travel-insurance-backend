package com.travel.insurance.procedure.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a procedure. Deliberately excludes the normalized name (an
 * internal duplicate-checking field). {@code id} is the procedure's public id.
 */
public record ProcedureResponse(
        UUID id,
        String procedureCode,
        String name,
        String description,
        UUID departmentPublicId,
        boolean active,
        UUID uploadBatchPublicId,
        Instant createdDate,
        Instant updatedDate
) {
}
