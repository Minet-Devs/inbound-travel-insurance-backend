package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BenefitServiceImpl implements BenefitService {

    private final BenefitRepository benefitRepository;
    private final BenefitMapper benefitMapper;
    private final PolicyService policyService;

    @Override
    public List<BenefitResponse> create(List<BenefitRequest> requests) {
        Set<String> seenInBatch = new HashSet<>();
        for (BenefitRequest request : requests) {
            policyService.getEntityById(request.policyId());
            if (!seenInBatch.add(request.policyId() + ":" + request.name().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "Duplicate benefit in request for the same policy: " + request.name());
            }
            if (benefitRepository.existsByPolicyIdAndNameIgnoreCase(request.policyId(), request.name())) {
                throw new IllegalStateException(
                        "Benefit already exists for this policy: " + request.name());
            }
        }
        List<Benefit> saved = benefitRepository.saveAll(
                requests.stream().map(benefitMapper::toEntity).toList());
        return saved.stream().map(benefitMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BenefitResponse getById(UUID id) {
        return benefitMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BenefitResponse> list(UUID policyId, Pageable pageable) {
        Page<Benefit> page = policyId != null
                ? benefitRepository.findAllByPolicyId(policyId, pageable)
                : benefitRepository.findAll(pageable);
        return page.map(benefitMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BenefitResponse> listAllByPolicy(UUID policyId) {
        return benefitRepository.findAllByPolicyId(policyId).stream()
                .map(benefitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> namesByIds(Collection<UUID> benefitIds) {
        if (benefitIds.isEmpty()) {
            return Map.of();
        }
        return benefitRepository.findAllById(benefitIds).stream()
                .collect(Collectors.toMap(Benefit::getId, Benefit::getName));
    }

    @Override
    public BenefitResponse update(UUID id, BenefitRequest request) {
        Benefit benefit = getEntityById(id);
        if (benefitRepository.existsByPolicyIdAndNameIgnoreCaseAndIdNot(
                request.policyId(), request.name(), id)) {
            throw new IllegalStateException(
                    "Benefit already exists for this policy: " + request.name());
        }
        benefitMapper.updateEntity(benefit, request);
        return benefitMapper.toResponse(benefitRepository.save(benefit));
    }

    @Override
    public void delete(UUID id) {
        benefitRepository.delete(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Benefit getEntityById(UUID id) {
        return benefitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Benefit", id));
    }
}
