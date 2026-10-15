package com.travel.insurance.claim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Converts an approved pre-authorization into a claim. Everything else (policy, visitor,
 * benefit, provider, diagnosis, amount, line items) is copied from the pre-authorization.
 */
public record ConvertPreauthorizationRequest(
        @NotBlank @Size(max = 100) String invoiceNumber,
        LocalDate issueDate
) {
}
