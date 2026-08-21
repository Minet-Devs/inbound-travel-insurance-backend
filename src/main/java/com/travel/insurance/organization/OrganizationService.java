package com.travel.insurance.organization;

import com.travel.insurance.organization.dto.OrganizationRequest;
import com.travel.insurance.organization.dto.OrganizationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface OrganizationService {

    OrganizationResponse create(OrganizationRequest request);

    OrganizationResponse getById(UUID id);

    Page<OrganizationResponse> list(OrganizationType organizationType, Pageable pageable);

    OrganizationResponse update(UUID id, OrganizationRequest request);

    void delete(UUID id);

    Organization getEntityById(UUID id);

    Map<UUID, String> namesByIds(Collection<UUID> organizationIds);
}
