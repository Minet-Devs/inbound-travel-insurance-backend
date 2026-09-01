package com.travel.insurance.visitor;

import com.travel.insurance.visitor.dto.VisitorEntryExitUpdate;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import com.travel.insurance.visitor.dto.VisitorStatusUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitorService {

    VisitorResponse create(VisitorRequest request);

    VisitorResponse getById(UUID id);

    List<VisitorResponse> listByPolicyId(UUID policyId);

    VisitorResponse getByPassportNumber(String passportNumber);

    Page<VisitorResponse> list(UUID insurerId, Pageable pageable);

    VisitorResponse update(UUID id, VisitorRequest request);

    void delete(UUID id);

    Visitor getEntityById(UUID id);

    Visitor getEntityByPassportNumber(String passportNumber);

    Optional<Visitor> findByEmail(String email);

    VisitorResponse updateVisitorStatus(UUID id, VisitorStatusUpdate visitorStatusUpdate);

    VisitorResponse updateVisitorStatusByPassportNumber(String passportNumber,
                                                        VisitorStatusUpdate visitorStatusUpdate);

    VisitorResponse updateEntryExitByPassportNumber(String passportNumber,
                                                     VisitorEntryExitUpdate entryExitUpdate);
}
