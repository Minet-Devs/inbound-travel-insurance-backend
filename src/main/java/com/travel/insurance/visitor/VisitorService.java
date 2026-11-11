package com.travel.insurance.visitor;

import com.travel.insurance.visitor.dto.AgeGroupVisitorCount;
import com.travel.insurance.visitor.dto.InsurerAgeGroupVisitorCount;
import com.travel.insurance.visitor.dto.CountryVisitorCount;
import com.travel.insurance.visitor.dto.InsurerVisitorCount;
import com.travel.insurance.visitor.dto.VisitorEntryExitUpdate;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import com.travel.insurance.visitor.dto.VisitorStatusUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface VisitorService {

    VisitorResponse create(VisitorRequest request);

    VisitorResponse getById(UUID id);

    /** ISO 3166-1 alpha-3 codes of nationalities exempt from the inbound policy, sorted. */
    List<String> getExemptNationalities();

    List<VisitorResponse> listByPolicyId(UUID policyId);

    VisitorResponse getByPassportNumber(String passportNumber);

    /** Exact passport match via the blind index; empty when no visitor has it. */
    Optional<UUID> findIdByPassportNumber(String passportNumber);

    Page<VisitorResponse> list(UUID insurerId, Pageable pageable);

    List<VisitorResponse> listForExport(UUID insurerId, VisitorStatus status,
                                        LocalDate dateFrom, LocalDate dateTo);

    /**
     * Total visitors (any status) per insurer; every insurer is listed, including those with zero.
     * With a {@code month} (1-12) only visitors registered in that month are counted, in
     * {@code year} (defaults to the current year); a {@code year} without a {@code month} is rejected.
     */
    List<InsurerVisitorCount> countByInsurer(Integer month, Integer year);

    /**
     * Total visitors (any status) per nationality (ISO alpha-3, upper-cased), ordered by country code.
     * With a {@code country} only that nationality is returned (case-insensitive; a zero row when it has
     * no visitors). {@code month}/{@code year} restrict to visitors registered in that month, as in
     * {@link #countByInsurer}.
     */
    List<CountryVisitorCount> countByCountry(String country, Integer month, Integer year);

    /**
     * Total visitors (any status) per age group, by age today: "0-2" (0 to under 3),
     * "3-17" (3 to under 18), "18+" and "Unknown" (age not populated). All four groups are
     * always returned, in that order, so they sum to the total visitor count.
     */
    List<AgeGroupVisitorCount> countByAgeGroup();

    /**
     * Age-group breakdown (same "0-2", "3-17", "18+", "Unknown" buckets, always all four) per insurer, any
     * visitor status. Every insurer is listed, including those with zero visitors.
     */
    List<InsurerAgeGroupVisitorCount> countByInsurerAndAgeGroup();

    /** Nationality (as stored) by visitor id for the given ids; unknown ids are absent from the map. */
    Map<UUID, String> nationalitiesByIds(Collection<UUID> visitorIds);

    VisitorResponse update(UUID id, VisitorRequest request);

    void delete(UUID id);

    Visitor getEntityById(UUID id);

    void markActivationEmailSent(UUID id);

    /** Flags the visitor as synced to Minet ({@code sent_to_minet = true}). */
    void markSentToMinet(UUID id);

    /**
     * IDs of ACTIVE visitors created in {@code (createdAfter, createdBefore)} whose activation
     * email has not been sent yet, newest first, at most {@code limit}.
     */
    List<UUID> findIdsAwaitingActivationEmail(Instant createdAfter, Instant createdBefore, int limit);

    Visitor getEntityByPassportNumber(String passportNumber);

    Optional<Visitor> findByEmail(String email);

    VisitorResponse updateVisitorStatus(UUID id, VisitorStatusUpdate visitorStatusUpdate);

    VisitorResponse updateVisitorStatusByPassportNumber(String passportNumber,
                                                        VisitorStatusUpdate visitorStatusUpdate);

    VisitorResponse updateEntryExitByPassportNumber(String passportNumber,
                                                     VisitorEntryExitUpdate entryExitUpdate);
}
