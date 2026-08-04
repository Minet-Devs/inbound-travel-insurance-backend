package com.travel.insurance.visitorbenefit;

import com.travel.insurance.visitor.VisitorStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VisitorStatusChangedListener {

    private final VisitorBenefitRepository visitorBenefitRepository;

    @EventListener
    @Transactional
    public void onVisitorStatusChanged(VisitorStatusChangedEvent event) {
        visitorBenefitRepository.findAllByVisitorId(event.visitorId())
                .forEach(visitorBenefit -> visitorBenefit.setStatus(event.newStatus()));
    }
}
