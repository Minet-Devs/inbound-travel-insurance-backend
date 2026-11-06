package com.travel.insurance.preauthorization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

public interface PreauthorizationRepository extends JpaRepository<Preauthorization, UUID> {

    Page<Preauthorization> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);

    Page<Preauthorization> findAllByPolicyId(UUID policyId, Pageable pageable);

    Page<Preauthorization> findAllByInsurerId(UUID insurerId, Pageable pageable);

    Page<Preauthorization> findAllByVisitorId(UUID visitorId, Pageable pageable);

    boolean existsByVisitorIdAndServiceProviderIdAndBenefitIdAndTreatmentDateAndRequestedAmount(
            UUID visitorId, UUID serviceProviderId, UUID benefitId, LocalDate treatmentDate,
            BigDecimal requestedAmount);

    @Query("SELECT p.status AS status, COUNT(p) AS statusCount FROM Preauthorization p "
            + "WHERE (:insurerId IS NULL OR p.insurerId = :insurerId) "
            + "AND (:serviceProviderId IS NULL OR p.serviceProviderId = :serviceProviderId) "
            + "AND p.createdDate >= COALESCE(:dateFrom, p.createdDate) "
            + "AND p.createdDate <= COALESCE(:dateTo, p.createdDate) "
            + "GROUP BY p.status")
    List<PreauthorizationStatusCount> countByStatus(@Param("insurerId") UUID insurerId,
                                                    @Param("serviceProviderId") UUID serviceProviderId,
                                                    @Param("dateFrom") Instant dateFrom,
                                                    @Param("dateTo") Instant dateTo);

    @Query("SELECT p.insurerId AS groupId, COUNT(p) AS preauthorizationCount, "
            + "COALESCE(SUM(p.approvedAmount), 0) AS approvedAmount "
            + "FROM Preauthorization p WHERE (:insurerId IS NULL OR p.insurerId = :insurerId) "
            + "AND (:serviceProviderId IS NULL OR p.serviceProviderId = :serviceProviderId) "
            + "AND p.createdDate >= COALESCE(:dateFrom, p.createdDate) "
            + "AND p.createdDate <= COALESCE(:dateTo, p.createdDate) "
            + "AND p.status IN :statuses GROUP BY p.insurerId")
    List<PreauthorizationDashboardTotal> summarizeByInsurer(@Param("statuses") Collection<PreauthorizationStatus> statuses,
                                                            @Param("insurerId") UUID insurerId,
                                                            @Param("serviceProviderId") UUID serviceProviderId,
                                                            @Param("dateFrom") Instant dateFrom,
                                                            @Param("dateTo") Instant dateTo);

    @Query("SELECT p.serviceProviderId AS groupId, COUNT(p) AS preauthorizationCount, "
            + "COALESCE(SUM(p.approvedAmount), 0) AS approvedAmount "
            + "FROM Preauthorization p WHERE (:insurerId IS NULL OR p.insurerId = :insurerId) "
            + "AND (:serviceProviderId IS NULL OR p.serviceProviderId = :serviceProviderId) "
            + "AND p.createdDate >= COALESCE(:dateFrom, p.createdDate) "
            + "AND p.createdDate <= COALESCE(:dateTo, p.createdDate) "
            + "AND p.status IN :statuses GROUP BY p.serviceProviderId")
    List<PreauthorizationDashboardTotal> summarizeByServiceProvider(
            @Param("statuses") Collection<PreauthorizationStatus> statuses,
            @Param("insurerId") UUID insurerId,
            @Param("serviceProviderId") UUID serviceProviderId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo);

    Page<Preauthorization> findAllByServiceProviderIdAndInsurerId(
            UUID serviceProviderId, UUID insurerId, Pageable pageable);
}
