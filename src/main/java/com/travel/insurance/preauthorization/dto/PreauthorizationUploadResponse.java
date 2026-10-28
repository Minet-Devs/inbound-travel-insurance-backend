package com.travel.insurance.preauthorization.dto;

import java.util.List;

public record PreauthorizationUploadResponse(
        int totalRows,
        int createdRows,
        int failedRows,
        List<PreauthorizationUploadRowResult> rows
) {
}
