package com.travel.insurance.claim.dto;

import com.travel.insurance.common.domain.VisitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Converts an approved pre-authorization into a claim. Everything else (policy, visitor,
 * benefit, provider, diagnosis, amount, line items) is copied from the pre-authorization.
 * {@code visitType} is optional: when given it overrides the pre-authorization's visit type,
 * otherwise the pre-authorization's value is copied.
 */
public record ConvertPreauthorizationRequest(
        @NotBlank @Size(max = 100) String invoiceNumber,
        LocalDate issueDate,
        VisitType visitType
) {
}
