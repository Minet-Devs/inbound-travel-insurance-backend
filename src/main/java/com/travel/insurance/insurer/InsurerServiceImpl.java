package com.travel.insurance.insurer;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.policy.PolicyService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InsurerServiceImpl implements InsurerService {

    private final InsurerRepository insurerRepository;
    private final InsurerMapper insurerMapper;
    private final PolicyService policyService;
    private final OrganizationService organizationService;
    private final ApplicationEventPublisher eventPublisher;

    public InsurerServiceImpl(InsurerRepository insurerRepository, InsurerMapper insurerMapper,
                               @Lazy PolicyService policyService, OrganizationService organizationService,
                               ApplicationEventPublisher eventPublisher) {
        this.insurerRepository = insurerRepository;
        this.insurerMapper = insurerMapper;
        this.policyService = policyService;
        this.organizationService = organizationService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public InsurerResponse create(InsurerRequest request) {
        if (insurerRepository.existsByName(request.name())) {
            throw new IllegalStateException("Insurer already exists: " + request.name());
        }
        validateOrganization(request.organizationId());
        Insurer insurer = insurerRepository.save(insurerMapper.toEntity(request));
        eventPublisher.publishEvent(new InsurerCreatedEvent(insurer.getId()));
        return toResponse(insurer);
    }

    @Override
    @Transactional(readOnly = true)
    public InsurerResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Insurer getEntityById(UUID id) {
        return getEntity(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InsurerResponse> list(Pageable pageable) {
        return insurerRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsurerResponse> listAll() {
        return insurerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public InsurerResponse update(UUID id, InsurerRequest request) {
        Insurer insurer = getEntity(id);
        validateOrganization(request.organizationId());
        insurerMapper.updateEntity(insurer, request);
        return toResponse(insurerRepository.save(insurer));
    }

    private void validateOrganization(UUID organizationId) {
        if (organizationId != null) {
            organizationService.getEntityById(organizationId);
        }
    }

    private InsurerResponse toResponse(Insurer insurer) {
        UUID policyId = policyService.findPolicyIdByInsurerId(insurer.getId()).orElse(null);
        return insurerMapper.toResponse(insurer, policyId);
    }

    @Override
    public void delete(UUID id) {
        insurerRepository.delete(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return insurerRepository.existsById(id);
    }

    @Override
    public void assignOrganizationId(UUID id, UUID organizationId) {
        Insurer insurer = getEntity(id);
        insurer.setOrganizationId(organizationId);
        insurerRepository.save(insurer);
    }

    private Insurer getEntity(UUID id) {
        return insurerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurer", id));
    }
}
