package com.travel.insurance.claim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    @Query("SELECT c FROM Claim c WHERE c.serviceProviderId = :providerId "
            + "AND c.status = COALESCE(:status, c.status) "
            + "AND c.createdDate >= COALESCE(:dateFrom, c.createdDate) "
            + "AND c.createdDate <= COALESCE(:dateTo, c.createdDate) "
            + "ORDER BY c.createdDate DESC")
    Page<Claim> findProviderClaims(@Param("providerId") UUID providerId,
                                   @Param("status") ClaimStatus status,
                                   @Param("dateFrom") Instant dateFrom,
                                   @Param("dateTo") Instant dateTo,
                                   Pageable pageable);

    @Query("SELECT c FROM Claim c WHERE c.serviceProviderId = :providerId "
            + "AND c.status = COALESCE(:status, c.status) "
            + "AND c.createdDate >= COALESCE(:dateFrom, c.createdDate) "
            + "AND c.createdDate <= COALESCE(:dateTo, c.createdDate) "
            + "ORDER BY c.createdDate DESC")
    List<Claim> findProviderClaimsAll(@Param("providerId") UUID providerId,
                                      @Param("status") ClaimStatus status,
                                      @Param("dateFrom") Instant dateFrom,
                                      @Param("dateTo") Instant dateTo);

    @Query("SELECT COALESCE(SUM(c.claimedAmount), 0) FROM Claim c "
            + "WHERE c.serviceProviderId = :providerId "
            + "AND c.status = COALESCE(:status, c.status) "
            + "AND c.createdDate >= COALESCE(:dateFrom, c.createdDate) "
            + "AND c.createdDate <= COALESCE(:dateTo, c.createdDate)")
    BigDecimal sumClaimedAmountByProvider(@Param("providerId") UUID providerId,
                                          @Param("status") ClaimStatus status,
                                          @Param("dateFrom") Instant dateFrom,
                                          @Param("dateTo") Instant dateTo);

    @Query("SELECT COALESCE(SUM(c.approvedAmount), 0) FROM Claim c "
            + "WHERE c.serviceProviderId = :providerId "
            + "AND c.status IN ('APPROVED', 'PARTIALLY_APPROVED') "
            + "AND c.createdDate >= COALESCE(:dateFrom, c.createdDate) "
            + "AND c.createdDate <= COALESCE(:dateTo, c.createdDate)")
    BigDecimal sumApprovedAmountByProvider(@Param("providerId") UUID providerId,
                                           @Param("dateFrom") Instant dateFrom,
                                           @Param("dateTo") Instant dateTo);

    @Query("SELECT c.status, COUNT(c) FROM Claim c "
            + "WHERE c.serviceProviderId = :providerId "
            + "AND c.createdDate >= COALESCE(:dateFrom, c.createdDate) "
            + "AND c.createdDate <= COALESCE(:dateTo, c.createdDate) "
            + "GROUP BY c.status")
    List<Object[]> countByStatusForProvider(@Param("providerId") UUID providerId,
                                            @Param("dateFrom") Instant dateFrom,
                                            @Param("dateTo") Instant dateTo);
}
