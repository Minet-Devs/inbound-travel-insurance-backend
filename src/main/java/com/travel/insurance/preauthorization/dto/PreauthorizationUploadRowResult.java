package com.travel.insurance.preauthorization.dto;

import java.util.UUID;

public record PreauthorizationUploadRowResult(
        int excelRowNumber,
        String passportNumber,
        PreauthorizationUploadRowStatus status,
        UUID preauthorizationId,
        String errorMessage
) {
}
