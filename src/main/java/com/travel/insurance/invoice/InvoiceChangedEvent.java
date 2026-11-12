package com.travel.insurance.invoice;

import java.util.UUID;

/**
 * Published synchronously, inside the invoice transaction, whenever an invoice belonging to a claim is
 * created, updated or deleted. The claim feature listens to keep the claimed amount equal to the sum of
 * its invoices, and throws (rolling the invoice change back) if the claim is no longer editable.
 */
public record InvoiceChangedEvent(UUID claimId) {
}
