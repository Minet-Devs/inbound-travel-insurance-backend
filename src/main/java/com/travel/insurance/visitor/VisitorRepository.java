package com.travel.insurance.visitor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitorRepository extends JpaRepository<Visitor, UUID>, JpaSpecificationExecutor<Visitor> {

    List<Visitor> findAllByPolicyId(UUID policyId);

    Page<Visitor> findByInsurerId(UUID insurerId, Pageable pageable);

    Optional<Visitor> findByPassportNumberHash(String passportNumberHash);

    Optional<Visitor> findFirstByEmailHashOrderByCreatedDateDesc(String emailHash);

    List<Visitor> findByVisitorStatusAndActivationEmailSentAtIsNullAndCreatedDateBetween(
            VisitorStatus status, Instant createdAfter, Instant createdBefore, Pageable pageable);

    boolean existsByPassportNumberHash(String passportNumberHash);

    boolean existsByPassportNumberHashAndIdNot(String passportNumberHash, UUID id);

    @Query("select v.insurerId as insurerId, count(v) as total from Visitor v "
            + "where v.insurerId is not null group by v.insurerId")
    List<InsurerVisitorTotal> countVisitorsGroupedByInsurer();

    interface InsurerVisitorTotal {
        UUID getInsurerId();

        long getTotal();
    }

    @Query(value = "select nextval('certificate_serial_seq')", nativeQuery = true)
    long nextCertificateSerialValue();
}
