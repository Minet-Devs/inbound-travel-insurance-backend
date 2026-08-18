package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.claim.ClaimUtilizationTotal;
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

import java.math.BigDecimal;
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
    private final ClaimService claimService;

    @Override
    public VisitorBenefitResponse create(VisitorBenefitRequest request) {
        Visitor visitor = visitorService.getEntityById(request.visitorId());
        Benefit benefit = benefitService.getEntityById(request.benefitId());
        if (visitorBenefitRepository.existsByVisitorIdAndBenefitId(
                request.visitorId(), request.benefitId())) {
            throw new IllegalStateException(
                    "Benefit already assigned to this visitor: " + request.benefitId());
        }
        VisitorBenefit visitorBenefit = new VisitorBenefit();
        applyRequest(visitorBenefit, request, benefit, visitor);
        visitorBenefit = visitorBenefitRepository.save(visitorBenefit);
        return visitorBenefitMapper.toResponse(
                visitorBenefit, benefit.getBenefitName(), utilizedAmountFor(visitorBenefit));
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
        Benefit benefit = benefitService.getEntityById(request.benefitId());
        if (visitorBenefitRepository.existsByVisitorIdAndBenefitIdAndIdNot(
                request.visitorId(), request.benefitId(), id)) {
            throw new IllegalStateException(
                    "Benefit already assigned to this visitor: " + request.benefitId());
        }
        applyRequest(visitorBenefit, request, benefit, visitor);
        return visitorBenefitMapper.toResponse(visitorBenefit, benefit.getBenefitName(), utilizedAmountFor(visitorBenefit));
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
        Map<UUID, String> namesByIds = benefitService.namesByIds(benefitIds);
        Map<VisitorBenefitKey, BigDecimal> utilizedByKey = utilizedAmountsFor(visitorBenefits);
        return visitorBenefits.stream()
                .map(visitorBenefit ->  visitorBenefitMapper.toResponse(
                        visitorBenefit,
                        namesByIds.get(visitorBenefit.getBenefitId()),
                        utilizedByKey.getOrDefault(VisitorBenefitKey.of(visitorBenefit), BigDecimal.ZERO)))
                .toList();
    }

    private BigDecimal utilizedAmountFor(VisitorBenefit visitorBenefit) {
        return utilizedAmountsFor(List.of(visitorBenefit))
                .getOrDefault(VisitorBenefitKey.of(visitorBenefit), BigDecimal.ZERO);
    }

    private Map<VisitorBenefitKey, BigDecimal> utilizedAmountsFor(List<VisitorBenefit> visitorBenefits) {
        Set<UUID> visitorIds = visitorBenefits.stream()
                .map(VisitorBenefit::getVisitorId)
                .collect(Collectors.toSet());
        Set<UUID> benefitIds = visitorBenefits.stream()
                .map(VisitorBenefit::getBenefitId)
                .collect(Collectors.toSet());
        return claimService.sumClaimedAmountsByVisitorAndBenefit(visitorIds, benefitIds).stream()
                .collect(Collectors.toMap(
                        total -> new VisitorBenefitKey(total.visitorId(), total.benefitId()),
                        ClaimUtilizationTotal::totalClaimed));
    }

    private void applyRequest(VisitorBenefit visitorBenefit, VisitorBenefitRequest request,
                              Benefit benefit, Visitor visitor) {
        visitorBenefit.setVisitorId(request.visitorId());
        visitorBenefit.setBenefitId(request.benefitId());
        visitorBenefit.setLimitAmount(
                request.limitAmount() != null ? request.limitAmount() : benefit.getLimitAmount());
        visitorBenefit.setStatus(visitor.getVisitorStatus());
    }

    private record VisitorBenefitKey(UUID visitorId, UUID benefitId) {
        static VisitorBenefitKey of(VisitorBenefit visitorBenefit) {
            return new VisitorBenefitKey(visitorBenefit.getVisitorId(), visitorBenefit.getBenefitId());
        }
    }
}
