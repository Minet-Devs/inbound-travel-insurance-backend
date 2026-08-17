package com.travel.insurance.report;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.Claim;
import com.travel.insurance.claim.ClaimRepository;
import com.travel.insurance.claim.ClaimStatus;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.invoice.InvoiceService;
import com.travel.insurance.invoice.dto.InvoiceItemResponse;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.procedure.ProcedureService;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import com.travel.insurance.report.dto.ClaimReceiptResponse;
import com.travel.insurance.report.dto.ClaimInvoiceGroup;
import com.travel.insurance.report.dto.ProviderClaimReportResponse;
import com.travel.insurance.report.dto.ProviderClaimReportSummary;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private VisitorService visitorService;
    @Mock private BenefitService benefitService;
    @Mock private InvoiceService invoiceService;
    @Mock private Icd11CodeService icd11CodeService;
    @Mock private ProcedureService procedureService;
    @Mock private ServiceProviderService serviceProviderService;
    @Mock private SpringTemplateEngine templateEngine;

    @InjectMocks
    private ReportServiceImpl reportService;

    private final UUID claimId = UUID.randomUUID();
    private final UUID visitorId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID diagnosisId = UUID.randomUUID();
    private final UUID procedureId = UUID.randomUUID();

    @Test
    void getClaimReceipt_assemblesFullReceipt() {
        Claim claim = buildClaim();
        Visitor visitor = buildVisitor();
        InvoiceResponse invoice = buildInvoice();

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(visitorService.getEntityById(visitorId)).thenReturn(buildVisitor());
        when(benefitService.namesByIds(Set.of(benefitId))).thenReturn(Map.of(benefitId, "Medical Expenses"));
        when(invoiceService.getByIds(Set.of(invoiceId))).thenReturn(List.of(invoice));
        when(icd11CodeService.getById(diagnosisId)).thenReturn(new Icd11CodeResponse(diagnosisId, "1A00", "Cholera", Instant.now(), Instant.now()));
        when(procedureService.getById(procedureId)).thenReturn(new ProcedureResponse(procedureId, "P001", "Consultation", "General consultation", null, true, null, Instant.now(), Instant.now()));
        when(serviceProviderService.getById(providerId)).thenReturn(new com.travel.insurance.serviceprovider.dto.ServiceProviderResponse(providerId, "Nairobi Hospital", "info@nairobi.com", "+254700000000", "Nairobi", Instant.now(), Instant.now()));

        ClaimReceiptResponse receipt = reportService.getClaimReceipt(claimId);

        assertThat(receipt.claimId()).isEqualTo(claimId);
        assertThat(receipt.visitorName()).isEqualTo("John Doe");
        assertThat(receipt.passportNumber()).isEqualTo("AB123456");
        assertThat(receipt.benefitName()).isEqualTo("Medical Expenses");
        assertThat(receipt.diagnoses()).containsExactly("Cholera");
        assertThat(receipt.procedures()).containsExactly("Consultation");
        assertThat(receipt.invoices()).hasSize(1);
        assertThat(receipt.totalAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    void getClaimReceipt_throwsWhenClaimNotFound() {
        when(claimRepository.findById(claimId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reportService.getClaimReceipt(claimId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getClaimReceipt_handlesMissingVisitor() {
        Claim claim = buildClaim();
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(visitorService.getEntityById(visitorId)).thenThrow(new ResourceNotFoundException("Visitor", visitorId));
        when(benefitService.namesByIds(Set.of(benefitId))).thenReturn(Map.of(benefitId, "Medical"));
        when(invoiceService.getByIds(anySet())).thenReturn(List.of());
        when(icd11CodeService.getById(diagnosisId)).thenReturn(new Icd11CodeResponse(diagnosisId, "1A00", "Cholera", Instant.now(), Instant.now()));
        when(procedureService.getById(procedureId)).thenReturn(new ProcedureResponse(procedureId, "P001", "Consultation", "General consultation", null, true, null, Instant.now(), Instant.now()));
        when(serviceProviderService.getById(providerId)).thenReturn(new com.travel.insurance.serviceprovider.dto.ServiceProviderResponse(providerId, "Nairobi Hospital", "info@nairobi.com", "+254700000000", "Nairobi", Instant.now(), Instant.now()));

        ClaimReceiptResponse receipt = reportService.getClaimReceipt(claimId);

        assertThat(receipt.visitorName()).isEqualTo("Unknown");
        assertThat(receipt.passportNumber()).isEqualTo("N/A");
    }

    @Test
    void getClaimReceipt_fallsBackToClaimAmountWhenNoInvoices() {
        Claim claim = buildClaim();
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(visitorService.getEntityById(visitorId)).thenReturn(buildVisitor());
        when(benefitService.namesByIds(Set.of(benefitId))).thenReturn(Map.of(benefitId, "Medical"));
        when(invoiceService.getByIds(anySet())).thenReturn(List.of());
        when(icd11CodeService.getById(diagnosisId)).thenReturn(new Icd11CodeResponse(diagnosisId, "1A00", "Cholera", Instant.now(), Instant.now()));
        when(procedureService.getById(procedureId)).thenReturn(new ProcedureResponse(procedureId, "P001", "Consultation", "General consultation", null, true, null, Instant.now(), Instant.now()));
        when(serviceProviderService.getById(providerId)).thenReturn(new com.travel.insurance.serviceprovider.dto.ServiceProviderResponse(providerId, "Nairobi Hospital", "info@nairobi.com", "+254700000000", "Nairobi", Instant.now(), Instant.now()));

        ClaimReceiptResponse receipt = reportService.getClaimReceipt(claimId);

        assertThat(receipt.totalAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    void getProviderReport_returnsPaginatedResults() {
        Claim claim = buildClaim();
        Page<Claim> page = new PageImpl<>(List.of(claim), PageRequest.of(0, 20), 1);

        when(serviceProviderService.getById(providerId)).thenReturn(null);
        when(claimRepository.findProviderClaims(eq(providerId), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(claimRepository.sumClaimedAmountByProvider(eq(providerId), isNull(), isNull(), isNull())).thenReturn(new BigDecimal("50000.00"));
        when(claimRepository.sumApprovedAmountByProvider(eq(providerId), isNull(), isNull())).thenReturn(BigDecimal.ZERO);
        List<Object[]> statusCounts = List.<Object[]>of(new Object[]{ClaimStatus.SUBMITTED, 1L});
        when(claimRepository.countByStatusForProvider(eq(providerId), isNull(), isNull())).thenReturn(statusCounts);
        when(visitorService.getEntityById(visitorId)).thenReturn(buildVisitor());
        when(benefitService.namesByIds(Set.of(benefitId))).thenReturn(Map.of(benefitId, "Medical"));

        ProviderClaimReportResponse report = reportService.getProviderReport(providerId, null, null, null, PageRequest.of(0, 20));

        assertThat(report.summary().totalClaims()).isEqualTo(1);
        assertThat(report.claims()).hasSize(1);
        assertThat(report.claims().get(0).benefitName()).isEqualTo("Medical");
        assertThat(report.totalElements()).isEqualTo(1);
    }

    @Test
    void getProviderReport_withStatusFilter() {
        Page<Claim> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(serviceProviderService.getById(providerId)).thenReturn(null);
        when(claimRepository.findProviderClaims(eq(providerId), eq(ClaimStatus.APPROVED), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);
        when(claimRepository.sumClaimedAmountByProvider(eq(providerId), isNull(), isNull(), isNull())).thenReturn(BigDecimal.ZERO);
        when(claimRepository.sumApprovedAmountByProvider(eq(providerId), isNull(), isNull())).thenReturn(BigDecimal.ZERO);
        when(claimRepository.countByStatusForProvider(eq(providerId), isNull(), isNull())).thenReturn(List.<Object[]>of());

        ProviderClaimReportResponse report = reportService.getProviderReport(providerId, "APPROVED", null, null, PageRequest.of(0, 20));

        assertThat(report.summary().totalClaims()).isEqualTo(0);
        assertThat(report.claims()).isEmpty();
    }

    @Test
    void getProviderReport_withDateRange() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        Page<Claim> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(serviceProviderService.getById(providerId)).thenReturn(null);
        when(claimRepository.findProviderClaims(eq(providerId), isNull(), any(Instant.class), any(Instant.class), any(Pageable.class))).thenReturn(emptyPage);
        when(claimRepository.sumClaimedAmountByProvider(eq(providerId), isNull(), any(Instant.class), any(Instant.class))).thenReturn(BigDecimal.ZERO);
        when(claimRepository.sumApprovedAmountByProvider(eq(providerId), any(Instant.class), any(Instant.class))).thenReturn(BigDecimal.ZERO);
        when(claimRepository.countByStatusForProvider(eq(providerId), any(Instant.class), any(Instant.class))).thenReturn(List.<Object[]>of());

        ProviderClaimReportResponse report = reportService.getProviderReport(providerId, null, from, to, PageRequest.of(0, 20));

        assertThat(report.claims()).isEmpty();
        verify(claimRepository).findProviderClaims(eq(providerId), isNull(), any(Instant.class), any(Instant.class), any(Pageable.class));
    }

    @Test
    void generateClaimReceiptPdf_rendersPdf() {
        Claim claim = buildClaim();
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(visitorService.getEntityById(visitorId)).thenReturn(buildVisitor());
        when(benefitService.namesByIds(Set.of(benefitId))).thenReturn(Map.of(benefitId, "Medical"));
        when(invoiceService.getByIds(anySet())).thenReturn(List.of());
        when(icd11CodeService.getById(diagnosisId)).thenReturn(new Icd11CodeResponse(diagnosisId, "1A00", "Cholera", Instant.now(), Instant.now()));
        when(procedureService.getById(procedureId)).thenReturn(new ProcedureResponse(procedureId, "P001", "Consultation", "General consultation", null, true, null, Instant.now(), Instant.now()));
        when(serviceProviderService.getById(providerId)).thenReturn(new com.travel.insurance.serviceprovider.dto.ServiceProviderResponse(providerId, "Nairobi Hospital", "info@nairobi.com", "+254700000000", "Nairobi", Instant.now(), Instant.now()));
        when(templateEngine.process(eq("report/claim-receipt"), any())).thenReturn("<html><body>test</body></html>");

        byte[] pdf = reportService.generateClaimReceiptPdf(claimId);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
    }

    @Test
    void generateProviderReportPdf_rendersPdf() {
        when(serviceProviderService.getById(providerId)).thenReturn(null);
        when(claimRepository.findProviderClaimsAll(eq(providerId), isNull(), isNull(), isNull())).thenReturn(List.of());
        when(claimRepository.sumClaimedAmountByProvider(eq(providerId), isNull(), isNull(), isNull())).thenReturn(BigDecimal.ZERO);
        when(claimRepository.sumApprovedAmountByProvider(eq(providerId), isNull(), isNull())).thenReturn(BigDecimal.ZERO);
        when(claimRepository.countByStatusForProvider(eq(providerId), isNull(), isNull())).thenReturn(List.<Object[]>of());
        when(templateEngine.process(eq("report/provider-claims"), any())).thenReturn("<html><body>test</body></html>");

        byte[] pdf = reportService.generateProviderReportPdf(providerId, null, null, null);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
    }

    @Test
    void generateProviderReportExcel_rendersXlsx() {
        when(serviceProviderService.getById(providerId)).thenReturn(null);
        when(claimRepository.findProviderClaimsAll(eq(providerId), isNull(), isNull(), isNull())).thenReturn(List.of());
        when(claimRepository.sumClaimedAmountByProvider(eq(providerId), isNull(), isNull(), isNull())).thenReturn(BigDecimal.ZERO);
        when(claimRepository.sumApprovedAmountByProvider(eq(providerId), isNull(), isNull())).thenReturn(BigDecimal.ZERO);
        when(claimRepository.countByStatusForProvider(eq(providerId), isNull(), isNull())).thenReturn(List.<Object[]>of());

        byte[] excel = reportService.generateProviderReportExcel(providerId, null, null, null);

        assertThat(excel).isNotNull();
        assertThat(excel.length).isGreaterThan(0);
    }

    @Test
    void getProviderReport_throwsWhenProviderNotFound() {
        when(serviceProviderService.getById(providerId)).thenThrow(new ResourceNotFoundException("ServiceProvider", providerId));
        assertThatThrownBy(() -> reportService.getProviderReport(providerId, null, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Claim buildClaim() {
        Claim claim = new Claim();
        claim.setId(claimId);
        claim.setVisitorId(visitorId);
        claim.setBenefitId(benefitId);
        claim.setServiceProviderId(providerId);
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setClaimedAmount(new BigDecimal("50000.00"));
        claim.setCurrency("KES");
        claim.setInvoiceIds(new HashSet<>(Set.of(invoiceId)));
        claim.setDiagnosisIds(new HashSet<>(Set.of(diagnosisId)));
        claim.setProcedureIds(new HashSet<>(Set.of(procedureId)));
        return claim;
    }

    private Visitor buildVisitor() {
        Visitor visitor = new Visitor();
        visitor.setId(visitorId);
        visitor.setFullName("John Doe");
        visitor.setPassportNumber("AB123456");
        visitor.setDateOfBirth(LocalDate.of(1990, 5, 15));
        visitor.setNationality("Kenyan");
        visitor.setDateIn(LocalDate.of(2026, 3, 1));
        visitor.setDateOut(LocalDate.of(2026, 3, 15));
        return visitor;
    }

    private InvoiceResponse buildInvoice() {
        InvoiceItemResponse item = new InvoiceItemResponse(
                UUID.randomUUID(), null, null,
                "Consultation fee", BigDecimal.valueOf(2), new BigDecimal("7500.00"), new BigDecimal("15000.00"),
                null, null, LocalDate.of(2026, 3, 5));
        return new InvoiceResponse(
                invoiceId, claimId, "INV-001", LocalDate.of(2026, 3, 5),
                "KES", new BigDecimal("15000.00"), new BigDecimal("0.007734"), "USD",
                new BigDecimal("116.01"), LocalDateTime.now(), List.of(item),
                Instant.now(), Instant.now());
    }
}
