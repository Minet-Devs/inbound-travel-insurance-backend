package com.travel.insurance.department;

import com.travel.insurance.department.dto.DepartmentRequest;
import com.travel.insurance.department.dto.DepartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface DepartmentService {

    DepartmentResponse create(DepartmentRequest request);

    DepartmentResponse getById(UUID id);

    Page<DepartmentResponse> list(Pageable pageable);

    DepartmentResponse update(UUID id, DepartmentRequest request);

    void delete(UUID id);

    Department getEntityById(UUID id);

    boolean existsByName(String name);

    Department findOrCreateByName(String name);

    Map<UUID, String> namesByIds(Collection<UUID> departmentIds);

    /**
     * Resolves department ids by name, case-insensitively and ignoring surrounding
     * whitespace. The returned map is keyed by the lower-cased, trimmed name;
     * unknown names are simply absent. Used by the procedure Excel upload to map a
     * per-row department name to its id in one bulk query.
     */
    Map<String, UUID> idsByName(Collection<String> names);
}