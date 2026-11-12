package com.travel.insurance.preauthorization;

import com.travel.insurance.preauthorization.dto.CountryPreauthorizationTotal;
import com.travel.insurance.preauthorization.dto.InsurerPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.OfflineApproval;
import com.travel.insurance.preauthorization.dto.PreauthorizationDashboardSummary;
import com.travel.insurance.preauthorization.dto.ServiceProviderPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationUpdateRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PreauthorizationService {

    PreauthorizationResponse create(PreauthorizationRequest request);

    PreauthorizationResponse getById(UUID id);

    Page<PreauthorizationResponse> list(UUID insurerId, Pageable pageable);

    Page<PreauthorizationResponse> listByVisitor(UUID visitorId, Pageable pageable);

    /** Replaces the details of a still-PENDING pre-authorization, including its enhancement and line items. */
    PreauthorizationResponse update(UUID id, PreauthorizationUpdateRequest request);

    PreauthorizationResponse decide(UUID id, PreauthorizationDecisionRequest request);

    /**
     * Creates an already-approved pre-authorization from an offline approval (approved amount equals the
     * requested amount) in its own transaction. Rejects an inactive policy and a duplicate of an existing
     * pre-authorization (same visitor, provider, benefit, treatment date and amount). Returns its id.
     */
    UUID createOfflineApproval(OfflineApproval approval);

    /**
     * Dashboard counts by decision status over the optional {@code createdDate} range (inclusive ISO dates, UTC).
     * Insurer and provider users only count their own pre-authorizations.
     */
    PreauthorizationDashboardSummary dashboardSummary(LocalDate dateFrom, LocalDate dateTo);

    /** APPROVED + PARTIALLY_APPROVED per insurer; every (in-scope) insurer is listed, including those with none. */
    List<InsurerPreauthorizationDistribution> distributionByInsurer(LocalDate dateFrom, LocalDate dateTo);

    /**
     * APPROVED + PARTIALLY_APPROVED per service provider, top {@code limit} (default 10) by count, ties broken
     * by approved amount; providers with none are dropped.
     */
    List<ServiceProviderPreauthorizationDistribution> topServiceProviders(
            LocalDate dateFrom, LocalDate dateTo, Integer limit);

    /**
     * Pre-authorizations per visitor nationality (ISO alpha-3, upper-cased), ordered by country code: count plus
     * requested and approved amount sums. Optional {@code status} (default every status) and
     * {@code createdDate} range. Only countries with at least one pre-authorization are listed.
     */
    List<CountryPreauthorizationTotal> totalsByCountry(PreauthorizationStatus status, LocalDate dateFrom,
                                                       LocalDate dateTo);

    void delete(UUID id);

    Preauthorization getEntityById(UUID id);

    void markPreAuthorizationConvertedToClaim(UUID id);

    /**
     * Moves a pre-authorization to another service provider. Called when the claim it was converted
     * into is moved, so the pre-auth stays visible to (and reported under) the claim's provider.
     */
    void reassignServiceProvider(UUID id, UUID serviceProviderId);
}
