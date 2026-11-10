package com.travel.insurance.claim;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Map;

/** Aggregates over a filtered set of claims, for the claims list summary. */
public interface ClaimRepositoryCustom {

    /** Statuses with no matching claims are absent from the map. */
    Map<ClaimStatus, Long> countByStatus(Specification<Claim> spec);

    /** Sum of {@code claimedAmountBase}; zero when nothing matches. */
    BigDecimal sumClaimedAmountBase(Specification<Claim> spec);
}
