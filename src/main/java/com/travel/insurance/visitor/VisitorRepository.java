package com.travel.insurance.visitor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitorRepository extends JpaRepository<Visitor, UUID> {

    List<Visitor> findAllByPolicyId(UUID policyId);

    Page<Visitor> findByInsurerId(UUID insurerId, Pageable pageable);

    Optional<Visitor> findByPassportNumberHash(String passportNumberHash);

    boolean existsByPassportNumberHash(String passportNumberHash);

    boolean existsByPassportNumberHashAndIdNot(String passportNumberHash, UUID id);
}
