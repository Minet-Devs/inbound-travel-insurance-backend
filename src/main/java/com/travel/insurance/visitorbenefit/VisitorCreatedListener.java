package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VisitorCreatedListener {

    private final VisitorBenefitRepository visitorBenefitRepository;
    private final BenefitService benefitService;

    @EventListener
    @Transactional
    public void onVisitorCreated(VisitorCreatedEvent event) {
        List<VisitorBenefit> assignments = benefitService.listAllByPolicy(event.policyId()).stream()
                .map(benefit -> {
                    VisitorBenefit visitorBenefit = new VisitorBenefit();
                    visitorBenefit.setVisitorId(event.visitorId());
                    visitorBenefit.setBenefitId(benefit.id());
                    visitorBenefit.setLimitAmount(benefit.limitAmount());
                    return visitorBenefit;
                })
                .toList();
        if (!assignments.isEmpty()) {
            visitorBenefitRepository.saveAll(assignments);
        }
    }
}
