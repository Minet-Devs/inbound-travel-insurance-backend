package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.BenefitType;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitRequest;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        Visitor visitor = visitorService.getEntityById(request.visitorId());
        Benefit benefit = validateAssignment(visitor, request);
        if (visitorBenefitRepository.existsByVisitorIdAndBenefitId(
                request.visitorId(), request.benefitId())) {
            throw new IllegalStateException(
                    "Benefit already assigned to this visitor: " + request.benefitId());
        }
        VisitorBenefit visitorBenefit = new VisitorBenefit();
        applyRequest(visitorBenefit, request, benefit, visitor);
        return visitorBenefitMapper.toResponse(
                visitorBenefitRepository.save(visitorBenefit), benefit.getBenefitType());
    }

    @Override
    @Transactional(readOnly = true)
    public VisitorBenefitResponse getById(UUID id) {
        return toResponses(List.of(getEntityById(id))).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VisitorBenefitResponse> list(UUID visitorId, Pageable pageable) {
        Page<VisitorBenefit> page = visitorId != null
                ? visitorBenefitRepository.findAllByVisitorId(visitorId, pageable)
                : visitorBenefitRepository.findAll(pageable);
        return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorBenefitResponse> listAllByVisitor(UUID visitorId) {
        return toResponses(visitorBenefitRepository.findAllByVisitorId(visitorId));
    }

    @Override
    public VisitorBenefitResponse update(UUID id, VisitorBenefitRequest request) {
        VisitorBenefit visitorBenefit = getEntityById(id);
        Visitor visitor = visitorService.getEntityById(request.visitorId());
        Benefit benefit = validateAssignment(visitor, request);
        if (visitorBenefitRepository.existsByVisitorIdAndBenefitIdAndIdNot(
                request.visitorId(), request.benefitId(), id)) {
            throw new IllegalStateException(
                    "Benefit already assigned to this visitor: " + request.benefitId());
        }
        applyRequest(visitorBenefit, request, benefit, visitor);
        return visitorBenefitMapper.toResponse(visitorBenefit, benefit.getBenefitType());
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

    private List<VisitorBenefitResponse> toResponses(List<VisitorBenefit> visitorBenefits) {
        Set<UUID> benefitIds = visitorBenefits.stream()
                .map(VisitorBenefit::getBenefitId)
                .collect(Collectors.toSet());
        Map<UUID, BenefitType> typesByIds = benefitService.typesByIds(benefitIds);
        return visitorBenefits.stream()
                .map(visitorBenefit -> visitorBenefitMapper.toResponse(
                        visitorBenefit, typesByIds.get(visitorBenefit.getBenefitId())))
                .toList();
    }

    private Benefit validateAssignment(Visitor visitor, VisitorBenefitRequest request) {
        Benefit benefit = benefitService.getEntityById(request.benefitId());
        if (!benefit.getPolicyId().equals(visitor.getPolicyId())) {
            throw new IllegalStateException(
                    "Benefit does not belong to the visitor's policy: " + request.benefitId());
        }
        return benefit;
    }

    private void applyRequest(VisitorBenefit visitorBenefit, VisitorBenefitRequest request,
                              Benefit benefit, Visitor visitor) {
        visitorBenefit.setVisitorId(request.visitorId());
        visitorBenefit.setBenefitId(request.benefitId());
        visitorBenefit.setLimitAmount(
                request.limitAmount() != null ? request.limitAmount() : benefit.getLimitAmount());
        visitorBenefit.setStatus(visitor.getVisitorStatus());
    }
}
