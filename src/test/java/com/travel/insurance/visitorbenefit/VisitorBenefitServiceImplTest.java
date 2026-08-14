package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.claim.ClaimUtilizationTotal;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitRequest;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorBenefitServiceImplTest {

    @Mock
    private VisitorBenefitRepository visitorBenefitRepository;

    @Mock
    private VisitorService visitorService;

    @Mock
    private BenefitService benefitService;

    @Mock
    private ClaimService claimService;

    private final VisitorBenefitMapper visitorBenefitMapper = new VisitorBenefitMapper();

    private VisitorBenefitServiceImpl visitorBenefitService;

    private UUID visitorId;
    private UUID benefitId;
    private Visitor visitor;
    private Benefit benefit;

    @BeforeEach
    void setUp() {
        visitorBenefitService = new VisitorBenefitServiceImpl(
                visitorBenefitRepository, visitorBenefitMapper, visitorService, benefitService, claimService);
        visitorId = UUID.randomUUID();
        benefitId = UUID.randomUUID();
        visitor = new Visitor();
        visitor.setPolicyId(UUID.randomUUID());
        benefit = new Benefit();
        benefit.setBenefitName("Medical Expenses");
        benefit.setLimitAmount(new BigDecimal("100000.00"));
    }

    @Test
    void createSnapshotsLimitFromBenefitWhenNotProvided() {
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor);
        when(benefitService.getEntityById(benefitId)).thenReturn(benefit);
        when(visitorBenefitRepository.existsByVisitorIdAndBenefitId(visitorId, benefitId))
                .thenReturn(false);
        when(visitorBenefitRepository.save(any(VisitorBenefit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VisitorBenefitResponse response = visitorBenefitService.create(
                new VisitorBenefitRequest(visitorId, benefitId, null));

        assertThat(response.limitAmount()).isEqualByComparingTo("100000.00");
        assertThat(response.visitorId()).isEqualTo(visitorId);
        assertThat(response.benefitId()).isEqualTo(benefitId);
        assertThat(response.benefitName()).isEqualTo("Medical Expenses");
    }

    @Test
    void listAllByVisitorResolvesBenefitNames() {
        VisitorBenefit visitorBenefit = new VisitorBenefit();
        visitorBenefit.setVisitorId(visitorId);
        visitorBenefit.setBenefitId(benefitId);
        visitorBenefit.setLimitAmount(new BigDecimal("100000.00"));
        when(visitorBenefitRepository.findAllByVisitorId(visitorId))
                .thenReturn(List.of(visitorBenefit));
        when(benefitService.namesByIds(Set.of(benefitId)))
                .thenReturn(Map.of(benefitId, "Medical Expenses"));
        when(claimService.sumClaimedAmountsByVisitorAndBenefit(Set.of(visitorId), Set.of(benefitId)))
                .thenReturn(List.of(new ClaimUtilizationTotal(visitorId, benefitId, new BigDecimal("30000.00"))));

        List<VisitorBenefitResponse> responses = visitorBenefitService.listAllByVisitor(visitorId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().benefitName()).isEqualTo("Medical Expenses");
        assertThat(responses.getFirst().limitAmount()).isEqualByComparingTo("100000.00");
        assertThat(responses.getFirst().utilizedAmount()).isEqualByComparingTo("30000.00");
        assertThat(responses.getFirst().balance()).isEqualByComparingTo("70000.00");
    }

    @Test
    void listAllByVisitorDefaultsUtilizedAmountToZeroWhenNoClaims() {
        VisitorBenefit visitorBenefit = new VisitorBenefit();
        visitorBenefit.setVisitorId(visitorId);
        visitorBenefit.setBenefitId(benefitId);
        visitorBenefit.setLimitAmount(new BigDecimal("100000.00"));
        when(visitorBenefitRepository.findAllByVisitorId(visitorId))
                .thenReturn(List.of(visitorBenefit));
        when(benefitService.namesByIds(Set.of(benefitId)))
                .thenReturn(Map.of(benefitId, "Medical Expenses"));
        when(claimService.sumClaimedAmountsByVisitorAndBenefit(Set.of(visitorId), Set.of(benefitId)))
                .thenReturn(List.of());

        List<VisitorBenefitResponse> responses = visitorBenefitService.listAllByVisitor(visitorId);

        assertThat(responses.getFirst().utilizedAmount()).isEqualByComparingTo("0");
        assertThat(responses.getFirst().balance()).isEqualByComparingTo("100000.00");
    }

    @Test
    void createUsesExplicitLimitWhenProvided() {
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor);
        when(benefitService.getEntityById(benefitId)).thenReturn(benefit);
        when(visitorBenefitRepository.existsByVisitorIdAndBenefitId(visitorId, benefitId))
                .thenReturn(false);
        when(visitorBenefitRepository.save(any(VisitorBenefit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VisitorBenefitResponse response = visitorBenefitService.create(
                new VisitorBenefitRequest(visitorId, benefitId, new BigDecimal("50000.00")));

        assertThat(response.limitAmount()).isEqualByComparingTo("50000.00");
    }

    @Test
    void createRejectsDuplicateAssignment() {
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor);
        when(benefitService.getEntityById(benefitId)).thenReturn(benefit);
        when(visitorBenefitRepository.existsByVisitorIdAndBenefitId(visitorId, benefitId))
                .thenReturn(true);

        assertThatThrownBy(() -> visitorBenefitService.create(
                new VisitorBenefitRequest(visitorId, benefitId, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(benefitId.toString());
        verify(visitorBenefitRepository, never()).save(any());
    }
}
