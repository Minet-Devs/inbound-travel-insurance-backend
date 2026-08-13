package com.travel.insurance.claim.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AttachInvoiceRequest(
        @NotNull(message = "invoiceId is required") UUID invoiceId
) {
}
