package com.travel.insurance.claim;

import com.travel.insurance.invoice.Invoice;
import com.travel.insurance.visitor.Visitor;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Building blocks for the filtered claims list. {@link #needsAttention} reads {@link Visitor} and
 * {@link Invoice} through read-only correlated subqueries — the one place the claim feature looks at
 * another feature's tables directly, because the flags must be filterable and countable in SQL.
 */
final class ClaimSpecifications {

    private ClaimSpecifications() {
    }

    static Specification<Claim> insurer(UUID insurerId) {
        return (root, query, cb) -> cb.equal(root.get("insurerId"), insurerId);
    }

    static Specification<Claim> serviceProvider(UUID serviceProviderId) {
        return (root, query, cb) -> cb.equal(root.get("serviceProviderId"), serviceProviderId);
    }

    static Specification<Claim> statusIn(Collection<ClaimStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /** {@code from} inclusive, {@code toExclusive} exclusive; either may be null. */
    static Specification<Claim> createdBetween(Instant from, Instant toExclusive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), from));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdDate"), toExclusive));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    static Specification<Claim> attributeIn(String attribute, Collection<UUID> ids) {
        return (root, query, cb) -> root.get(attribute).in(ids);
    }

    /** Listed providers, plus claims with no provider when {@code includeUnlinked}. */
    static Specification<Claim> providers(Collection<UUID> serviceProviderIds, boolean includeUnlinked) {
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            if (serviceProviderIds != null && !serviceProviderIds.isEmpty()) {
                any.add(root.get("serviceProviderId").in(serviceProviderIds));
            }
            if (includeUnlinked) {
                any.add(cb.isNull(root.get("serviceProviderId")));
            }
            return cb.or(any.toArray(new Predicate[0]));
        };
    }

    static Specification<Claim> amountBaseBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (min != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("claimedAmountBase"), min));
            }
            if (max != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("claimedAmountBase"), max));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    static Specification<Claim> sentToMinet(boolean sent) {
        return (root, query, cb) -> cb.equal(root.get("sentToMinet"), sent);
    }

    /**
     * Free-text search, resolved by the caller into ids via the owning features' services: a claim
     * matches if it is one of {@code claimIds}, belongs to one of {@code visitorIds}, or carries one of
     * {@code diagnosisIds}. No ids at all means nothing matches.
     */
    static Specification<Claim> search(Collection<UUID> claimIds, Collection<UUID> visitorIds,
                                       Collection<UUID> diagnosisIds) {
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            if (!claimIds.isEmpty()) {
                any.add(root.get("id").in(claimIds));
            }
            if (!visitorIds.isEmpty()) {
                any.add(root.get("visitorId").in(visitorIds));
            }
            if (!diagnosisIds.isEmpty()) {
                Subquery<UUID> withDiagnosis = query.subquery(UUID.class);
                Root<Claim> claim = withDiagnosis.from(Claim.class);
                Join<Claim, UUID> diagnosis = claim.join("diagnosisIds");
                withDiagnosis.select(claim.get("id")).where(diagnosis.in(diagnosisIds));
                any.add(root.get("id").in(withDiagnosis));
            }
            return any.isEmpty() ? cb.disjunction() : cb.or(any.toArray(new Predicate[0]));
        };
    }

    /** Claims carrying any of {@code flags}; see {@link ClaimAttentionFlag}. */
    static Specification<Claim> needsAttention(Collection<ClaimAttentionFlag> flags) {
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            for (ClaimAttentionFlag flag : flags) {
                switch (flag) {
                    case NO_PREAUTH -> any.add(cb.isNull(root.get("preauthorizationId")));
                    case OUTSIDE_STAY -> {
                        Subquery<UUID> outside = query.subquery(UUID.class);
                        Root<Visitor> visitor = outside.from(Visitor.class);
                        Expression<LocalDate> submitted = root.get("createdDate").as(LocalDate.class);
                        outside.select(visitor.get("id")).where(
                                cb.equal(visitor.get("id"), root.get("visitorId")),
                                cb.or(cb.lessThan(submitted, visitor.<LocalDate>get("dateIn")),
                                        cb.greaterThan(submitted, visitor.<LocalDate>get("dateOut"))));
                        any.add(cb.exists(outside));
                    }
                    case INVOICE_MISMATCH -> {
                        Subquery<BigDecimal> invoiced = query.subquery(BigDecimal.class);
                        Root<Invoice> invoice = invoiced.from(Invoice.class);
                        invoiced.select(cb.sum(invoice.<BigDecimal>get("totalAmount")))
                                .where(cb.equal(invoice.get("claimId"), root.get("id")));
                        // SUM over no invoices is NULL, so claims without invoices never match.
                        any.add(cb.notEqual(invoiced, root.<BigDecimal>get("claimedAmount")));
                    }
                }
            }
            return cb.or(any.toArray(new Predicate[0]));
        };
    }
}
