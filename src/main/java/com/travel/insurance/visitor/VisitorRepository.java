package com.travel.insurance.visitor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitorRepository extends JpaRepository<Visitor, UUID> {

    List<Visitor> findAllByPolicyId(UUID policyId);

    Optional<Visitor> findByPassportNumberIgnoreCase(String passportNumber);

    boolean existsByPassportNumberIgnoreCase(String passportNumber);

    boolean existsByPassportNumberIgnoreCaseAndIdNot(String passportNumber, UUID id);
}
