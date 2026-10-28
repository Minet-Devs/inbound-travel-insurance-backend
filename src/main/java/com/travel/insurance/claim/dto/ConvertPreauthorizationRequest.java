package com.travel.insurance.claim.dto;

import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Converts an approved pre-authorization into a claim. The caller supplies the invoice and its
 * hospital service lines; the lines' total becomes the claimed amount and may not exceed the
 * pre-auth's approved amount. Everything else (policy, visitor, benefit, provider, diagnosis)
 * is copied from the pre-authorization.
 */
public record ConvertPreauthorizationRequest(
        @NotBlank @Size(max = 100) String invoiceNumber,
        LocalDate issueDate,
        @NotEmpty @Valid List<InvoiceItemRequest> invoiceItems
) {
}
