package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitRequest;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitorBenefitServiceImpl implements VisitorBenefitService {

    private final VisitorBenefitRepository visitorBenefitRepository;
    private final VisitorBenefitMapper visitorBenefitMapper;
    private final VisitorService visitorService;
    private final BenefitService benefitService;

    @Override
    public VisitorBenefitResponse create(VisitorBenefitRequest request) {
        Benefit benefit = validateAssignment(request);
        if (visitorBenefitRepository.existsByVisitorIdAndBenefitId(
                request.visitorId(), request.benefitId())) {
            throw new IllegalStateException(
                    "Benefit already assigned to this visitor: " + request.benefitId());
        }
        VisitorBenefit visitorBenefit = new VisitorBenefit();
        applyRequest(visitorBenefit, request, benefit);
        return visitorBenefitMapper.toResponse(visitorBenefitRepository.save(visitorBenefit));
    }

    @Override
    @Transactional(readOnly = true)
    public VisitorBenefitResponse getById(UUID id) {
        return visitorBenefitMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VisitorBenefitResponse> list(UUID visitorId, Pageable pageable) {
        Page<VisitorBenefit> page = visitorId != null
                ? visitorBenefitRepository.findAllByVisitorId(visitorId, pageable)
                : visitorBenefitRepository.findAll(pageable);
        return page.map(visitorBenefitMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorBenefitResponse> listAllByVisitor(UUID visitorId) {
        return visitorBenefitRepository.findAllByVisitorId(visitorId).stream()
                .map(visitorBenefitMapper::toResponse)
                .toList();
    }

    @Override
    public VisitorBenefitResponse update(UUID id, VisitorBenefitRequest request) {
        VisitorBenefit visitorBenefit = getEntityById(id);
        Benefit benefit = validateAssignment(request);
        if (visitorBenefitRepository.existsByVisitorIdAndBenefitIdAndIdNot(
                request.visitorId(), request.benefitId(), id)) {
            throw new IllegalStateException(
                    "Benefit already assigned to this visitor: " + request.benefitId());
        }
        applyRequest(visitorBenefit, request, benefit);
        return visitorBenefitMapper.toResponse(visitorBenefitRepository.save(visitorBenefit));
    }

    @Override
    public void delete(UUID id) {
        visitorBenefitRepository.delete(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public VisitorBenefit getEntityById(UUID id) {
        return visitorBenefitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VisitorBenefit", id));
    }

    private Benefit validateAssignment(VisitorBenefitRequest request) {
        Visitor visitor = visitorService.getEntityById(request.visitorId());
        Benefit benefit = benefitService.getEntityById(request.benefitId());
        if (!benefit.getPolicyId().equals(visitor.getPolicyId())) {
            throw new IllegalStateException(
                    "Benefit does not belong to the visitor's policy: " + request.benefitId());
        }
        return benefit;
    }

    private void applyRequest(VisitorBenefit visitorBenefit, VisitorBenefitRequest request,
                              Benefit benefit) {
        visitorBenefit.setVisitorId(request.visitorId());
        visitorBenefit.setBenefitId(request.benefitId());
        visitorBenefit.setLimitAmount(
                request.limitAmount() != null ? request.limitAmount() : benefit.getLimitAmount());
    }
}
