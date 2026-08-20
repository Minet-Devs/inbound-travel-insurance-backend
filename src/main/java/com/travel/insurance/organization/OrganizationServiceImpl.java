package com.travel.insurance.organization;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.organization.dto.OrganizationRequest;
import com.travel.insurance.organization.dto.OrganizationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    @Override
    public OrganizationResponse create(OrganizationRequest request) {
        if (organizationRepository.existsByName(request.name())) {
            throw new IllegalStateException("Organization already exists: " + request.name());
        }
        Organization organization = organizationMapper.toEntity(request);
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getById(UUID id) {
        return organizationMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationResponse> list(OrganizationType organizationType, Pageable pageable) {
        Page<Organization> organizations = organizationType == null
                ? organizationRepository.findAll(pageable)
                : organizationRepository.findByOrganizationType(organizationType, pageable);
        return organizations.map(organizationMapper::toResponse);
    }

    @Override
    public OrganizationResponse update(UUID id, OrganizationRequest request) {
        Organization organization = getEntityById(id);
        if (organizationRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalStateException("Organization already exists: " + request.name());
        }
        organizationMapper.updateEntity(organization, request);
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Override
    public void delete(UUID id) {
        organizationRepository.delete(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getEntityById(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
    }
}
