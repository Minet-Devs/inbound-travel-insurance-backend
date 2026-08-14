package com.travel.insurance.claim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    Page<Claim> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);

    Page<Claim> findAllByPolicyId(UUID policyId, Pageable pageable);

    @Query("SELECT new com.travel.insurance.claim.ClaimUtilizationTotal(c.visitorId, c.benefitId, SUM(c.claimedAmount)) "
            + "FROM Claim c "
            + "WHERE c.visitorId IN :visitorIds AND c.benefitId IN :benefitIds "
            + "GROUP BY c.visitorId, c.benefitId")
    List<ClaimUtilizationTotal> sumClaimedAmountsByVisitorAndBenefit(
            @Param("visitorIds") Collection<UUID> visitorIds,
            @Param("benefitIds") Collection<UUID> benefitIds);
}
