package com.travel.insurance.procedure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Input for manual creation and updates. The caller supplies only the name,
 * optional description and department; the procedure code, public id, normalized
 * name, source, upload-batch id and audit fields are all set by the backend.
 */
public record ProcedureRequest(
        @NotBlank String name,
        String description,
        @NotNull UUID departmentPublicId
) {
}
