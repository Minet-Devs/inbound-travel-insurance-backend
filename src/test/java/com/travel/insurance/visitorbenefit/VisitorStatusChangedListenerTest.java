package com.travel.insurance.visitorbenefit;

import com.travel.insurance.visitor.VisitorStatus;
import com.travel.insurance.visitor.VisitorStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorStatusChangedListenerTest {

    @Mock
    private VisitorBenefitRepository visitorBenefitRepository;

    @InjectMocks
    private VisitorStatusChangedListener listener;

    private final UUID visitorId = UUID.randomUUID();

    private VisitorBenefit benefitWithStatus(VisitorStatus status) {
        VisitorBenefit visitorBenefit = new VisitorBenefit();
        visitorBenefit.setVisitorId(visitorId);
        visitorBenefit.setBenefitId(UUID.randomUUID());
        visitorBenefit.setStatus(status);
        return visitorBenefit;
    }

    @Test
    void mirrorsNewStatusOntoAllVisitorBenefits() {
        VisitorBenefit first = benefitWithStatus(VisitorStatus.PENDING);
        VisitorBenefit second = benefitWithStatus(VisitorStatus.PENDING);
        when(visitorBenefitRepository.findAllByVisitorId(visitorId))
                .thenReturn(List.of(first, second));

        listener.onVisitorStatusChanged(
                new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        assertThat(first.getStatus()).isEqualTo(VisitorStatus.ACTIVE);
        assertThat(second.getStatus()).isEqualTo(VisitorStatus.ACTIVE);
    }

    @Test
    void doesNothingWhenVisitorHasNoBenefits() {
        when(visitorBenefitRepository.findAllByVisitorId(visitorId)).thenReturn(List.of());

        listener.onVisitorStatusChanged(
                new VisitorStatusChangedEvent(visitorId, VisitorStatus.SUSPENDED));
    }
}
