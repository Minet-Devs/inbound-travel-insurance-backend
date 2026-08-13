package com.travel.insurance.medicalservice.dto;

public record MedicalServiceImportResult(
        int totalRows,
        int departmentsCreated,
        int servicesInserted,
        int servicesSkipped
) {
}