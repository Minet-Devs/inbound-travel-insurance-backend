package com.travel.insurance.icd11.dto;

public record Icd11ImportResult(
        int totalRows,
        int inserted,
        int updated,
        int skipped
) {
}
