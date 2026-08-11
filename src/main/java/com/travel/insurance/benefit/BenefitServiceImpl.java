package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
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
    public BenefitResponse create(BenefitRequest request) {
        return benefitMapper.toResponse(benefitRepository.save(benefitMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public BenefitResponse getById(UUID id) {
        return benefitMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BenefitResponse> list(Pageable pageable) {
        return benefitRepository.findAll(pageable).map(benefitMapper::toResponse);
    }

    @Override
    public BenefitResponse update(UUID id, BenefitRequest request) {
        Benefit benefit = getEntityById(id);
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

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> namesByIds(Collection<UUID> benefitIds) {
        if (benefitIds.isEmpty()) {
            return Map.of();
        }
        return benefitRepository.findAllById(benefitIds).stream()
                .collect(Collectors.toMap(Benefit::getId, Benefit::getBenefitName));
    }
}
