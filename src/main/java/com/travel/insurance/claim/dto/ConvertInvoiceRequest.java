package com.travel.insurance.claim.dto;

import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** One hospital invoice raised while converting a pre-authorization into a claim. */
public record ConvertInvoiceRequest(
        @NotBlank @Size(max = 100) String invoiceNumber,
        LocalDate issueDate,
        @NotEmpty @Valid List<InvoiceItemRequest> invoiceItems
) {
}
