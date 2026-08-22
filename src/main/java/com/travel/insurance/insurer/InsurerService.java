package com.travel.insurance.insurer;

import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface InsurerService {

    InsurerResponse create(InsurerRequest request);

    InsurerResponse getById(UUID id);

    Insurer getEntityById(UUID id);

    Page<InsurerResponse> list(Pageable pageable);

    List<InsurerResponse> listAll();

    InsurerResponse update(UUID id, InsurerRequest request);

    void delete(UUID id);

    boolean exists(UUID id);

    Map<UUID, String> namesByIds(Collection<UUID> insurerIds);

    Optional<UUID> findIdByOrganizationId(UUID organizationId);
}
