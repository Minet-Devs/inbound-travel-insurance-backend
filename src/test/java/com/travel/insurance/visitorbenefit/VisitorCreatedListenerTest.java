package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.BenefitType;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorCreatedListenerTest {

    @Mock
    private VisitorBenefitRepository visitorBenefitRepository;

    @Mock
    private BenefitService benefitService;

    @InjectMocks
    private VisitorCreatedListener listener;

    private final UUID visitorId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();

    @Test
    void seedsVisitorBenefitsForEachPolicyBenefit() {
        UUID inpatientId = UUID.randomUUID();
        UUID outpatientId = UUID.randomUUID();
        when(benefitService.listAllByPolicy(policyId)).thenReturn(List.of(
                new BenefitResponse(inpatientId, policyId, BenefitType.MEDICAL_EXPENSES,
                        new BigDecimal("100000.00"), Instant.now(), Instant.now()),
                new BenefitResponse(outpatientId, policyId, BenefitType.EMERGENCY_MEDICAL_EVACUATION,
                        new BigDecimal("25000.00"), Instant.now(), Instant.now())));

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VisitorBenefit>> captor = ArgumentCaptor.forClass(List.class);
        verify(visitorBenefitRepository).saveAll(captor.capture());
        List<VisitorBenefit> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(vb -> assertThat(vb.getVisitorId()).isEqualTo(visitorId));
        assertThat(saved).extracting(VisitorBenefit::getBenefitId)
                .containsExactlyInAnyOrder(inpatientId, outpatientId);
        assertThat(saved).extracting(VisitorBenefit::getLimitAmount)
                .containsExactlyInAnyOrder(new BigDecimal("100000.00"), new BigDecimal("25000.00"));
    }

    @Test
    void savesNothingWhenPolicyHasNoBenefits() {
        when(benefitService.listAllByPolicy(policyId)).thenReturn(List.of());

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(visitorBenefitRepository, never()).saveAll(any());
    }
}
