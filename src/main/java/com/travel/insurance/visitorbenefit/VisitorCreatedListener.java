package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions a {@link VisitorBenefit} for every benefit in the global catalog
 * whenever a visitor is created, so a new visitor starts with the full set of
 * benefits (each at the catalog limit and in {@code PENDING} status).
 */
@Component
@RequiredArgsConstructor
public class VisitorCreatedListener {

    private final VisitorBenefitRepository visitorBenefitRepository;
    private final BenefitService benefitService;

    @EventListener
    @Transactional
    public void onVisitorCreated(VisitorCreatedEvent event) {
        for (BenefitResponse benefit : benefitService.listAll()) {
            if (visitorBenefitRepository.existsByVisitorIdAndBenefitId(
                    event.visitorId(), benefit.id())) {
                continue;
            }
            VisitorBenefit visitorBenefit = new VisitorBenefit();
            visitorBenefit.setVisitorId(event.visitorId());
            visitorBenefit.setBenefitId(benefit.id());
            visitorBenefit.setLimitAmount(benefit.limitAmount());
            visitorBenefitRepository.save(visitorBenefit);
        }
    }
}
