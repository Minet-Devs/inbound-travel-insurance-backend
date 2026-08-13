package com.travel.insurance.procedure;

import com.travel.insurance.procedure.dto.ProcedureRequest;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProcedureService {

    ProcedureResponse create(ProcedureRequest request);

    ProcedureResponse getById(UUID id);

    Page<ProcedureResponse> list(String search, UUID departmentPublicId, Boolean active, Pageable pageable);

    ProcedureResponse update(UUID id, ProcedureRequest request);

    ProcedureResponse activate(UUID id);

    ProcedureResponse deactivate(UUID id);
}
