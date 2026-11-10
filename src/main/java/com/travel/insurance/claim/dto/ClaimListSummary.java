package com.travel.insurance.claim.dto;

import com.travel.insurance.claim.ClaimStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Header figures for the claims list, computed over the same filters as the list but ignoring
 * {@code status} (so every status tab can show its count).
 *
 * @param total            claims matching the filters, any status
 * @param countsByStatus   every {@link ClaimStatus}, zero when none match
 * @param awaitingDecision claims still {@code OPEN}, {@code SUBMITTED} or {@code UNDER_REVIEW}
 * @param totalValueBase   sum of {@code claimedAmountBase} (USD) over all matching claims
 * @param needsAttention   awaiting claims carrying at least one {@code ClaimAttentionFlag}
 * @param notSentToMinet   matching claims Minet does not have yet ({@code sentToMinet = false})
 */
public record ClaimListSummary(
        long total,
        Map<ClaimStatus, Long> countsByStatus,
        long awaitingDecision,
        BigDecimal totalValueBase,
        long needsAttention,
        long notSentToMinet
) {
}
