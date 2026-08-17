package com.travel.insurance.memberstatement;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.claim.ClaimStatus;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.memberstatement.dto.MemberStatementResponse;
import com.travel.insurance.memberstatement.dto.MemberStatementTransaction;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.VisitorStatus;
import com.travel.insurance.visitorbenefit.VisitorBenefitService;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberStatementServiceImplTest {

    @Mock
    private VisitorService visitorService;

    @Mock
    private VisitorBenefitService visitorBenefitService;

    @Mock
    private PolicyService policyService;

    @Mock
    private ClaimService claimService;

    @Mock
    private BenefitService benefitService;

    @Mock
    private ServiceProviderService serviceProviderService;

    @Mock
    private MemberStatementExcelWriter excelWriter;

    @Mock
    private MemberStatementPdfRenderer pdfRenderer;

    private MemberStatementServiceImpl memberStatementService;

    private UUID visitorId;
    private UUID policyId;
    private UUID benefitId;
    private UUID serviceProviderId;
    private Visitor visitor;
    private Policy policy;

    @BeforeEach
    void setUp() {
        memberStatementService = new MemberStatementServiceImpl(visitorService, visitorBenefitService,
                policyService, claimService, benefitService, serviceProviderService, excelWriter, pdfRenderer);

        visitorId = UUID.randomUUID();
        policyId = UUID.randomUUID();
        benefitId = UUID.randomUUID();
        serviceProviderId = UUID.randomUUID();

        visitor = new Visitor();
        visitor.setPolicyId(policyId);
        visitor.setFullName("Jane Traveler");
        visitor.setPassportNumber("P1234567");

        policy = new Policy();
        policy.setPolicyNumber("POL-0001");
    }

    @Test
    void getStatementIncludesAllTransactionsUnfiltered() {
        when(visitorService.getEntityByPassportNumber("P1234567")).thenReturn(visitor);
        when(policyService.getEntityById(policyId)).thenReturn(policy);
        when(visitorBenefitService.listAllByVisitor(any())).thenReturn(List.of());
        InvoiceResponse invoice = invoice("INV-001", LocalDate.of(2020, 1, 1), new BigDecimal("500.00"));
        ClaimResponse claim = claim(benefitId, serviceProviderId, List.of(invoice));
        when(claimService.listByVisitor(any())).thenReturn(List.of(claim));
        when(benefitService.namesByIds(Set.of(benefitId))).thenReturn(Map.of(benefitId, "Medical Expenses"));
        when(serviceProviderService.namesByIds(Set.of(serviceProviderId)))
                .thenReturn(Map.of(serviceProviderId, "Nairobi Hospital"));

        MemberStatementResponse statement = memberStatementService.getStatement("P1234567");

        assertThat(statement.memberName()).isEqualTo("Jane Traveler");
        assertThat(statement.passportNumber()).isEqualTo("P1234567");
        assertThat(statement.policyNumber()).isEqualTo("POL-0001");
        assertThat(statement.transactions()).hasSize(1);
        MemberStatementTransaction transaction = statement.transactions().getFirst();
        assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(transaction.benefitName()).isEqualTo("Medical Expenses");
        assertThat(transaction.amount()).isEqualByComparingTo("500.00");
        assertThat(transaction.invoiceNumber()).isEqualTo("INV-001");
        assertThat(transaction.serviceProviderName()).isEqualTo("Nairobi Hospital");
    }

    @Test
    void exportOnlyIncludesTransactionsWithinDateRange() {
        when(visitorService.getEntityByPassportNumber("P1234567")).thenReturn(visitor);
        when(policyService.getEntityById(policyId)).thenReturn(policy);
        when(visitorBenefitService.listAllByVisitor(any())).thenReturn(List.of());
        InvoiceResponse inRange = invoice("INV-001", LocalDate.of(2026, 6, 15), new BigDecimal("500.00"));
        InvoiceResponse outOfRange = invoice("INV-002", LocalDate.of(2025, 1, 1), new BigDecimal("100.00"));
        ClaimResponse claim = claim(benefitId, serviceProviderId, List.of(inRange, outOfRange));
        when(claimService.listByVisitor(any())).thenReturn(List.of(claim));
        when(benefitService.namesByIds(Set.of(benefitId))).thenReturn(Map.of(benefitId, "Medical Expenses"));
        when(serviceProviderService.namesByIds(Set.of(serviceProviderId)))
                .thenReturn(Map.of(serviceProviderId, "Nairobi Hospital"));
        when(excelWriter.write(any())).thenReturn(new byte[]{1, 2, 3});

        memberStatementService.export("P1234567", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                MemberStatementExportType.EXCEL);

        org.mockito.ArgumentCaptor<MemberStatementResponse> captor =
                org.mockito.ArgumentCaptor.forClass(MemberStatementResponse.class);
        verify(excelWriter).write(captor.capture());
        assertThat(captor.getValue().transactions()).hasSize(1);
        assertThat(captor.getValue().transactions().getFirst().invoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void exportSummaryReflectsAllTimeUtilizationRegardlessOfDateRange() {
        when(visitorService.getEntityByPassportNumber("P1234567")).thenReturn(visitor);
        when(policyService.getEntityById(policyId)).thenReturn(policy);
        VisitorBenefitResponse benefit = new VisitorBenefitResponse(UUID.randomUUID(), visitorId, benefitId,
                "Medical Expenses", new BigDecimal("100000.00"), new BigDecimal("38000.00"),
                new BigDecimal("62000.00"), VisitorStatus.ACTIVE, Instant.now(), Instant.now());
        when(visitorBenefitService.listAllByVisitor(any())).thenReturn(List.of(benefit));
        when(claimService.listByVisitor(any())).thenReturn(List.of());
        when(pdfRenderer.render(any())).thenReturn(new byte[]{1});

        memberStatementService.export("P1234567", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                MemberStatementExportType.PDF);

        org.mockito.ArgumentCaptor<MemberStatementResponse> captor =
                org.mockito.ArgumentCaptor.forClass(MemberStatementResponse.class);
        verify(pdfRenderer).render(captor.capture());
        VisitorBenefitResponse summary = captor.getValue().benefits().getFirst();
        assertThat(summary.utilizedAmount()).isEqualByComparingTo("38000.00");
        assertThat(summary.balance()).isEqualByComparingTo("62000.00");
    }

    @Test
    void exportRejectsFromDateAfterToDate() {
        assertThatThrownBy(() -> memberStatementService.export("P1234567",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), MemberStatementExportType.PDF))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ClaimResponse claim(UUID benefitId, UUID serviceProviderId, List<InvoiceResponse> invoices) {
        return new ClaimResponse(
                UUID.randomUUID(), policyId, benefitId, serviceProviderId, null,
                visitorId, null, null, null,
                new BigDecimal("500.00"), "KES", new BigDecimal("500.00"), BigDecimal.ONE,
                "USD", LocalDateTime.now(), null,
                "desc", null,
                List.of(), List.of(), invoices,
                Set.of(), null, ClaimStatus.SUBMITTED,
                Instant.now(), Instant.now());
    }

    private InvoiceResponse invoice(String invoiceNumber, LocalDate issueDate, BigDecimal totalAmount) {
        return new InvoiceResponse(
                UUID.randomUUID(), UUID.randomUUID(), invoiceNumber, issueDate,
                "KES", totalAmount, BigDecimal.ONE, "USD", totalAmount, LocalDateTime.now(),
                List.of(), Instant.now(), Instant.now());
    }
}