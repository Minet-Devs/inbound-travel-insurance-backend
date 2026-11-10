package com.travel.insurance.claim.dto;

import com.travel.insurance.claim.ClaimAttentionFlag;
import com.travel.insurance.claim.ClaimStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Optional filters for {@code GET /api/v1/claims} and {@code GET /api/v1/claims/summary}, bound from
 * query parameters. Every field is optional; list fields accept repeated or comma-separated values.
 *
 * <ul>
 *   <li>{@code q} — passport number (exact), invoice number (contains), ICD-11 code or title
 *       (contains), or a claim id. Visitor names are encrypted at rest and cannot be searched.</li>
 *   <li>{@code dateFrom}/{@code dateTo} — inclusive, on the claim's submitted date (UTC).</li>
 *   <li>{@code serviceProviderIds} and {@code providerUnlinked} are OR-ed: a claim matches if its
 *       provider is listed, or (when {@code providerUnlinked}) it has no provider.</li>
 *   <li>{@code minAmountBase}/{@code maxAmountBase} — inclusive, on {@code claimedAmountBase} (USD).</li>
 *   <li>{@code flags} — matches a claim with <em>any</em> of the listed flags.</li>
 * </ul>
 */
public record ClaimSearchCriteria(
        ClaimStatus status,
        String q,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        List<UUID> benefitIds,
        List<UUID> serviceProviderIds,
        Boolean providerUnlinked,
        List<UUID> insurerIds,
        BigDecimal minAmountBase,
        BigDecimal maxAmountBase,
        Boolean sentToMinet,
        List<ClaimAttentionFlag> flags
) {

    public static ClaimSearchCriteria none() {
        return new ClaimSearchCriteria(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public ClaimSearchCriteria withoutStatus() {
        return new ClaimSearchCriteria(null, q, dateFrom, dateTo, benefitIds, serviceProviderIds,
                providerUnlinked, insurerIds, minAmountBase, maxAmountBase, sentToMinet, flags);
    }

    public boolean hasSearch() {
        return q != null && !q.isBlank();
    }
}
