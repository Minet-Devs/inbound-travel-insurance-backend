package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.benefit.dto.BenefitTypeResponse;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
    public List<BenefitTypeResponse> listBenefitTypes() {
        return Arrays.stream(BenefitType.values())
                .map(type -> new BenefitTypeResponse(type, type.getMinimumLimit()))
                .toList();
    }

    @Override
    public List<BenefitResponse> create(List<BenefitRequest> requests) {
        Set<String> seenInBatch = new HashSet<>();
        for (BenefitRequest request : requests) {
            policyService.getEntityById(request.policyId());
            assertMeetsMinimumLimit(request.benefitType(), request.limitAmount());
            if (!seenInBatch.add(request.policyId() + ":" + request.benefitType())) {
                throw new IllegalArgumentException(
                        "Duplicate benefit in request for the same policy: " + request.benefitType());
            }
            if (benefitRepository.existsByPolicyIdAndBenefitType(request.policyId(), request.benefitType())) {
                throw new IllegalStateException(
                        "Benefit already exists for this policy: " + request.benefitType());
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
    public Map<UUID, BenefitType> typesByIds(Collection<UUID> benefitIds) {
        if (benefitIds.isEmpty()) {
            return Map.of();
        }
        return benefitRepository.findAllById(benefitIds).stream()
                .collect(Collectors.toMap(Benefit::getId, Benefit::getBenefitType));
    }

    @Override
    public BenefitResponse update(UUID id, BenefitRequest request) {
        Benefit benefit = getEntityById(id);
        assertMeetsMinimumLimit(request.benefitType(), request.limitAmount());
        if (benefitRepository.existsByPolicyIdAndBenefitTypeAndIdNot(
                request.policyId(), request.benefitType(), id)) {
            throw new IllegalStateException(
                    "Benefit already exists for this policy: " + request.benefitType());
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

    private void assertMeetsMinimumLimit(BenefitType benefitType, BigDecimal limitAmount) {
        BigDecimal minimum = benefitType.getMinimumLimit();
        if (limitAmount.compareTo(minimum) < 0) {
            throw new IllegalArgumentException(
                    "Limit amount for %s must be at least %s".formatted(benefitType, minimum));
        }
    }
}
