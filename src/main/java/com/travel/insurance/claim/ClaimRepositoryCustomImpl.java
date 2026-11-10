package com.travel.insurance.claim;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

class ClaimRepositoryCustomImpl implements ClaimRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<ClaimStatus, Long> countByStatus(Specification<Claim> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Claim> root = query.from(Claim.class);
        query.multiselect(root.get("status"), cb.count(root));
        applyWhere(spec, root, query, cb);
        query.groupBy(root.get("status"));
        Map<ClaimStatus, Long> counts = new EnumMap<>(ClaimStatus.class);
        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            counts.put(row.get(0, ClaimStatus.class), row.get(1, Long.class));
        }
        return counts;
    }

    @Override
    public BigDecimal sumClaimedAmountBase(Specification<Claim> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<Claim> root = query.from(Claim.class);
        query.select(cb.sum(root.<BigDecimal>get("claimedAmountBase")));
        applyWhere(spec, root, query, cb);
        BigDecimal sum = entityManager.createQuery(query).getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private static void applyWhere(Specification<Claim> spec, Root<Claim> root, CriteriaQuery<?> query,
                                   CriteriaBuilder cb) {
        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
    }
}
