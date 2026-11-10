package com.travel.insurance.claim.dto;

import com.travel.insurance.common.domain.VisitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Converts an approved pre-authorization into a claim. The caller supplies one or more invoices,
 * each with its hospital service lines; the total of all lines across all invoices becomes the
 * claimed amount and may not exceed the pre-auth's approved amount. Invoice numbers must be
 * unique within the request. Everything else (policy, visitor, benefit, provider, diagnosis)
 * is copied from the pre-authorization. {@code visitType} is optional: when given it overrides
 * the pre-authorization's visit type, otherwise the pre-authorization's value is copied.
 */
public record ConvertPreauthorizationRequest(
        @NotEmpty @Valid List<ConvertInvoiceRequest> invoices,
        VisitType visitType
) {
}
