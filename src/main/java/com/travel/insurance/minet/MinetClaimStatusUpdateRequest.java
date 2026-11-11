package com.travel.insurance.minet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * Status-change event pushed by Minet. Only {@code inboundClaimReference} (our claim id) is needed to find the
 * claim; every other field is optional. {@code status} is a free string (Minet has ~55 values).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MinetClaimStatusUpdateRequest(
        String eventId,
        String inboundClaimReference,
        String claimId,
        String claimNumber,
        String previousStatus,
        String status,
        Instant changedAt,
        List<String> comments) {
}
