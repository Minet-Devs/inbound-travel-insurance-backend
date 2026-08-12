package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.VisitorStatus;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorCreatedListenerTest {

    @Mock
    private VisitorBenefitRepository visitorBenefitRepository;

    @Mock
    private BenefitService benefitService;

    @Mock
    private VisitorService visitorService;

    @InjectMocks
    private VisitorCreatedListener listener;

    private final UUID visitorId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Visitor visitor = new Visitor();
        visitor.setVisitorStatus(VisitorStatus.ACTIVE);
        lenient().when(visitorService.getEntityById(visitorId)).thenReturn(visitor);
    }

    private BenefitResponse benefit(String name, BigDecimal limit) {
        return new BenefitResponse(UUID.randomUUID(), name, limit, Instant.now(), Instant.now());
    }

    @Test
    void provisionsAVisitorBenefitForEveryCatalogBenefitMatchingVisitorStatus() {
        BenefitResponse medical = benefit("Medical Expenses", new BigDecimal("20000.00"));
        BenefitResponse repatriation = benefit("Repatriation", new BigDecimal("5000.00"));
        when(benefitService.listAll()).thenReturn(List.of(medical, repatriation));
        when(visitorBenefitRepository.existsByVisitorIdAndBenefitId(any(), any())).thenReturn(false);

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        ArgumentCaptor<VisitorBenefit> captor = ArgumentCaptor.forClass(VisitorBenefit.class);
        verify(visitorBenefitRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(VisitorBenefit::getVisitorId, VisitorBenefit::getBenefitId,
                        VisitorBenefit::getLimitAmount, VisitorBenefit::getStatus)
                .containsExactly(
                        tuple(visitorId, medical.id(), new BigDecimal("20000.00"), VisitorStatus.ACTIVE),
                        tuple(visitorId, repatriation.id(), new BigDecimal("5000.00"), VisitorStatus.ACTIVE));
    }

    @Test
    void skipsBenefitsAlreadyAssignedToTheVisitor() {
        BenefitResponse medical = benefit("Medical Expenses", new BigDecimal("20000.00"));
        when(benefitService.listAll()).thenReturn(List.of(medical));
        when(visitorBenefitRepository.existsByVisitorIdAndBenefitId(visitorId, medical.id()))
                .thenReturn(true);

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(visitorBenefitRepository, never()).save(any());
    }

    @Test
    void savesNothingWhenCatalogIsEmpty() {
        when(benefitService.listAll()).thenReturn(List.of());

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(visitorBenefitRepository, never()).save(any());
    }
}
