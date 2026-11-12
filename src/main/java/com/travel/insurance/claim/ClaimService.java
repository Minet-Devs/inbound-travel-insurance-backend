package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.AttachInvoiceRequest;
import com.travel.insurance.claim.dto.ClaimListSummary;
import com.travel.insurance.claim.dto.ClaimSearchCriteria;
import com.travel.insurance.claim.dto.ClaimDashboardSummary;
import com.travel.insurance.claim.dto.CountryDiagnosisDistribution;
import com.travel.insurance.claim.dto.DiagnosisDistribution;
import com.travel.insurance.claim.dto.ClaimDecisionRequest;
import com.travel.insurance.claim.dto.ClaimExportRow;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimUpdateRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.claim.dto.ConvertPreauthorizationRequest;
import com.travel.insurance.claim.dto.InsurerClaimDistribution;
import com.travel.insurance.claim.dto.ServiceProviderClaimDistribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClaimService {

    ClaimResponse create(ClaimRequest request);

    ClaimResponse createFromPreauthorization(UUID preauthorizationId, ConvertPreauthorizationRequest request);

    /** Clinical details only; allowed while the claim status is editable (see {@link ClaimStatus#isEditable()}). */
    ClaimResponse update(UUID id, ClaimUpdateRequest request);

    ClaimResponse attachInvoice(UUID id, AttachInvoiceRequest request);

    ClaimResponse getById(UUID id);

    /**
     * Ids of all claims (including {@code claimId}) sharing its service provider, benefit and visitor;
     * empty when the claim does not exist.
     */
    List<UUID> findIdsWithSameProviderBenefitAndVisitor(UUID claimId);

    Claim getEntityById(UUID id);

    /** Flags the claim as sent to Minet ({@code sent_to_minet = true}). */
    void markSentToMinet(UUID id);

    /**
     * Applies a status update pushed by Minet to the claim with this id. Returns {@code false} (and changes
     * nothing) when the event was already processed or is older than the last applied update.
     */
    boolean applyMinetStatusUpdate(UUID id, String eventId, String status, Instant changedAt, List<String> comments);

    Page<ClaimResponse> list(Pageable pageable);

    /** Caller-scoped claims list (insurer/provider users see only their own) narrowed by {@code criteria}. */
    Page<ClaimResponse> search(ClaimSearchCriteria criteria, Pageable pageable);

    /** Header counts/KPIs for {@link #search} over the same filters, ignoring {@code criteria.status}. */
    ClaimListSummary summary(ClaimSearchCriteria criteria);

    List<ClaimResponse> listByVisitor(UUID visitorId);

    Page<ClaimResponse> listByVisitor(UUID visitorId, Pageable pageable);

    List<ClaimExportRow> listForExport(ClaimStatus status, UUID insurerId, LocalDate dateFrom, LocalDate dateTo);

    ClaimDashboardSummary dashboardSummary(ClaimStatus status, LocalDate dateFrom, LocalDate dateTo);

    /**
     * Claim count and percentage share per diagnosis (ICD-11), most frequent first, over the optional
     * {@code createdDate} range. A claim with several diagnoses counts once under each.
     */
    List<DiagnosisDistribution> diagnosisDistribution(LocalDate dateFrom, LocalDate dateTo);

    /**
     * Same as {@link #diagnosisDistribution} grouped by visitor nationality (ISO alpha-3, upper-cased), ordered
     * by country code; percentages are relative to each country.
     */
    List<CountryDiagnosisDistribution> diagnosisDistributionByCountry(LocalDate dateFrom, LocalDate dateTo);

    List<InsurerClaimDistribution> distributionByInsurer(ClaimStatus status, LocalDate dateFrom, LocalDate dateTo);

    List<ServiceProviderClaimDistribution> distributionByServiceProvider(
            ClaimStatus status, LocalDate dateFrom, LocalDate dateTo, Integer limit);

    ClaimResponse decide(UUID id, ClaimDecisionRequest request);

    ClaimResponse markPaid(UUID id);

    void delete(UUID id);

    List<ClaimUtilizationTotal> sumClaimedAmountsByVisitorAndBenefit(
            Collection<UUID> visitorIds, Collection<UUID> benefitIds);
}
