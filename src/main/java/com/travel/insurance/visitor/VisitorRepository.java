package com.travel.insurance.visitor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VisitorRepository extends JpaRepository<Visitor, UUID> {

    Optional<Visitor> findByPolicyId(UUID policyId);

    boolean existsByPolicyId(UUID policyId);

    boolean existsByPolicyIdAndIdNot(UUID policyId, UUID id);

    boolean existsByPassportNumberIgnoreCase(String passportNumber);

    boolean existsByPassportNumberIgnoreCaseAndIdNot(String passportNumber, UUID id);
}
