package com.travel.insurance.insurer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InsurerRepository extends JpaRepository<Insurer, UUID> {

    boolean existsByName(String name);

    Optional<Insurer> findFirstByOrganizationId(UUID organizationId);

    /**
     * Atomically takes one policy off the insurer's quota in a single statement, so concurrent
     * visitor creations cannot read the same value and lose a decrement.
     *
     * @return 1 if a token was consumed, 0 if the quota is null or already exhausted
     */
    @Modifying
    @Query("update Insurer i set i.policyToken = i.policyToken - 1, i.updatedDate = instant "
            + "where i.id = :id and i.policyToken > 0")
    int consumeOnePolicyToken(@Param("id") UUID id);

    /** Reads the current token straight from the database, bypassing any stale entity in the session. */
    @Query("select i.policyToken from Insurer i where i.id = :id")
    Optional<Long> findPolicyTokenById(@Param("id") UUID id);
}
