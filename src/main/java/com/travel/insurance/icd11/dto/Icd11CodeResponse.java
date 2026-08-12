package com.travel.insurance.icd11.dto;

import java.time.Instant;
import java.util.UUID;

public record Icd11CodeResponse(
        UUID id,
        String code,
        String title,
        Instant createdDate,
        Instant updatedDate
) {
}
