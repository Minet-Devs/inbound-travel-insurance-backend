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

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BenefitServiceImpl implements BenefitService {

    private final BenefitRepository benefitRepository;
    private final BenefitMapper benefitMapper;
    private final PolicyService policyService;

    @Override
    public BenefitResponse create(BenefitRequest request) {
        policyService.getEntityById(request.policyId());
        return benefitMapper.toResponse(benefitRepository.save(benefitMapper.toEntity(request)));
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
    public void drawDown(UUID benefitId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Draw-down amount must be positive");
        }
        Benefit benefit = getEntityById(benefitId);
        BigDecimal newUsed = benefit.getUsedAmount().add(amount);
        if (newUsed.compareTo(benefit.getLimitAmount()) > 0) {
            throw new IllegalStateException("Amount exceeds remaining benefit limit for " + benefit.getName());
        }
        benefit.setUsedAmount(newUsed);
        benefitRepository.save(benefit);
    }
}
