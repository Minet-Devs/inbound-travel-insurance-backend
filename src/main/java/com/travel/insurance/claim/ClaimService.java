package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.AttachInvoiceRequest;
import com.travel.insurance.claim.dto.ClaimDashboardSummary;
import com.travel.insurance.claim.dto.ClaimDecisionRequest;
import com.travel.insurance.claim.dto.ClaimExportRow;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.claim.dto.ConvertPreauthorizationRequest;
import com.travel.insurance.claim.dto.InsurerClaimDistribution;
import com.travel.insurance.claim.dto.ServiceProviderClaimDistribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClaimService {

    ClaimResponse create(ClaimRequest request);

    ClaimResponse createFromPreauthorization(UUID preauthorizationId, ConvertPreauthorizationRequest request);

    ClaimResponse update(UUID id, ClaimRequest request);

    ClaimResponse attachInvoice(UUID id, AttachInvoiceRequest request);

    ClaimResponse getById(UUID id);

    Claim getEntityById(UUID id);

    Page<ClaimResponse> list(Pageable pageable);

    List<ClaimResponse> listByVisitor(UUID visitorId);

    Page<ClaimResponse> listByVisitor(UUID visitorId, Pageable pageable);

    List<ClaimExportRow> listForExport(ClaimStatus status, UUID insurerId, LocalDate dateFrom, LocalDate dateTo);

    ClaimDashboardSummary dashboardSummary(ClaimStatus status, LocalDate dateFrom, LocalDate dateTo);

    List<InsurerClaimDistribution> distributionByInsurer(ClaimStatus status, LocalDate dateFrom, LocalDate dateTo);

    List<ServiceProviderClaimDistribution> distributionByServiceProvider(
            ClaimStatus status, LocalDate dateFrom, LocalDate dateTo, Integer limit);

    ClaimResponse decide(UUID id, ClaimDecisionRequest request);

    ClaimResponse markPaid(UUID id);

    void delete(UUID id);

    List<ClaimUtilizationTotal> sumClaimedAmountsByVisitorAndBenefit(
            Collection<UUID> visitorIds, Collection<UUID> benefitIds);
}
