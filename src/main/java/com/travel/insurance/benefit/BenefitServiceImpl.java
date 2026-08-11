package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.benefit.dto.BenefitTypeResponse;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BenefitServiceImpl implements BenefitService {

    private final BenefitRepository benefitRepository;
    private final BenefitMapper benefitMapper;

    @Override
    public List<BenefitTypeResponse> listBenefitTypes() {
        return Arrays.stream(BenefitType.values())
                .map(type -> new BenefitTypeResponse(type, type.getFixedLimit()))
                .toList();
    }

    @Override
    public List<BenefitResponse> provisionFixedBenefits(UUID policyId) {
        List<Benefit> toCreate = Arrays.stream(BenefitType.values())
                .filter(type -> !benefitRepository.existsByPolicyIdAndBenefitType(policyId, type))
                .map(type -> {
                    Benefit benefit = new Benefit();
                    benefit.setPolicyId(policyId);
                    benefit.setBenefitType(type);
                    benefit.setLimitAmount(type.getFixedLimit());
                    return benefit;
                })
                .toList();
        return benefitRepository.saveAll(toCreate).stream()
                .map(benefitMapper::toResponse)
                .toList();
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
    @Transactional(readOnly = true)
    public Benefit getEntityById(UUID id) {
        return benefitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Benefit", id));
    }
}
