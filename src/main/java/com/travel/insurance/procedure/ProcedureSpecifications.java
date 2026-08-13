package com.travel.insurance.procedure;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProcedureSpecifications {

    private ProcedureSpecifications() {
    }

    public static Specification<Procedure> filter(String search, UUID departmentPublicId, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("procedureCode")), like)));
            }
            if (departmentPublicId != null) {
                predicates.add(cb.equal(root.get("departmentPublicId"), departmentPublicId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
