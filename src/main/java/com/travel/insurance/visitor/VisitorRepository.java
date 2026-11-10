package com.travel.insurance.visitor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("select v.insurerId as insurerId, count(v) as total from Visitor v "
            + "where v.insurerId is not null and v.createdDate >= :from and v.createdDate < :to "
            + "group by v.insurerId")
    List<InsurerVisitorTotal> countVisitorsGroupedByInsurerCreatedBetween(
            @Param("from") Instant from, @Param("to") Instant to);

    interface InsurerVisitorTotal {
        UUID getInsurerId();

        long getTotal();
    }

    // Nationality is encrypted at rest, so it cannot be grouped in SQL; callers count in memory.
    @Query("select v.nationality from Visitor v")
    List<String> findAllNationalities();

    @Query("select v.nationality from Visitor v where v.createdDate >= :from and v.createdDate < :to")
    List<String> findNationalitiesCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select v.age as age, count(v) as total from Visitor v "
            + "group by v.age")
    List<AgeVisitorTotal> countVisitorsGroupedByAge();

    interface AgeVisitorTotal {
        Integer getAge();

        long getTotal();
    }

    @Query("select v.insurerId as insurerId, v.age as age, count(v) as total from Visitor v "
            + "where v.insurerId is not null group by v.insurerId, v.age")
    List<InsurerAgeVisitorTotal> countVisitorsGroupedByInsurerAndAge();

    interface InsurerAgeVisitorTotal {
        UUID getInsurerId();

        Integer getAge();

        long getTotal();
    }

    @Query(value = "select nextval('certificate_serial_seq')", nativeQuery = true)
    long nextCertificateSerialValue();
}
