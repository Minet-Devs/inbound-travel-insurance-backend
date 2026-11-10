package com.travel.insurance.claim;

/**
 * Checks an adjudicator should look at before deciding a claim. Computed in the claims list
 * filter ({@link ClaimSpecifications#needsAttention}), never stored.
 */
public enum ClaimAttentionFlag {
    /** Claim submitted (UTC date) before the visitor's {@code dateIn} or after their {@code dateOut}. */
    OUTSIDE_STAY,
    /** Claim not raised from a pre-authorization. */
    NO_PREAUTH,
    /** Claim has invoices whose combined {@code totalAmount} differs from its {@code claimedAmount}. */
    INVOICE_MISMATCH
}
