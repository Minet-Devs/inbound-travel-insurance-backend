package com.travel.insurance.visitor;

import com.travel.insurance.visitor.dto.VisitorEntryExitUpdate;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import com.travel.insurance.visitor.dto.VisitorStatusUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitorService {

    VisitorResponse create(VisitorRequest request);

    VisitorResponse getById(UUID id);

    List<VisitorResponse> listByPolicyId(UUID policyId);

    VisitorResponse getByPassportNumber(String passportNumber);

    Page<VisitorResponse> list(UUID insurerId, Pageable pageable);

    List<VisitorResponse> listForExport(UUID insurerId, VisitorStatus status,
                                        LocalDate dateFrom, LocalDate dateTo);

    VisitorResponse update(UUID id, VisitorRequest request);

    void delete(UUID id);

    Visitor getEntityById(UUID id);

    void markActivationEmailSent(UUID id);

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
