package com.travel.insurance.minet;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.claim.Claim;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.common.domain.VisitType;
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.invoice.InvoiceService;
import com.travel.insurance.invoice.dto.InvoiceItemResponse;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.dto.VisitorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinetClaimSyncServiceImplTest {

    @Mock private ClaimService claimService;
    @Mock private VisitorService visitorService;
    @Mock private ServiceProviderService serviceProviderService;
    @Mock private BenefitService benefitService;
    @Mock private Icd11CodeService icd11CodeService;
    @Mock private InvoiceService invoiceService;
    @Mock private MinetClient minetClient;

    @InjectMocks
    private MinetClaimSyncServiceImpl service;

    private final UUID claimId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private final UUID visitorId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();
    private final UUID diagnosisId = UUID.randomUUID();

    private Claim claim() {
        Claim claim = new Claim();
        claim.setId(claimId);
        claim.setServiceProviderId(providerId);
        claim.setVisitorId(visitorId);
        claim.setBenefitId(benefitId);
        claim.setVisitType(VisitType.OUTPATIENT);
        claim.setClaimedAmount(new BigDecimal("100000.00"));
        claim.setClaimedAmountBase(new BigDecimal("769.88"));
        claim.setExchangeRate(new BigDecimal("0.008000"));
        claim.getDiagnosisIds().add(diagnosisId);
        // 2026-11-05T22:30Z is already 06 Nov in Nairobi (UTC+3)
        claim.setCreatedDate(Instant.parse("2026-11-05T22:30:00Z"));
        return claim;
    }

    private ServiceProviderResponse provider(String minetId) {
        return new ServiceProviderResponse(providerId, "AGA KHAN HOSPITAL NAIROBI", null, null, null, null, null,
                null, null, Instant.now(), Instant.now(), minetId);
    }

    private VisitorResponse visitor() {
        return new VisitorResponse(visitorId, null, null, "Anna Smith", "7777", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, Instant.now(), Instant.now());
    }

    private InvoiceResponse invoice(String number, LocalDate issueDate, String total, LocalDate itemDate) {
        List<InvoiceItemResponse> items = itemDate == null ? List.of() : List.of(new InvoiceItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Consultation", "Consult", BigDecimal.ONE,
                new BigDecimal(total), new BigDecimal(total), null, null, itemDate));
        return new InvoiceResponse(UUID.randomUUID(), claimId, number, issueDate, "KES", new BigDecimal(total),
                null, "USD", null, LocalDateTime.now(), items, Instant.now(), Instant.now());
    }

    private void stubHappyPath() {
        when(claimService.getEntityById(claimId)).thenReturn(claim());
        when(serviceProviderService.getById(providerId)).thenReturn(provider("MSP-1001"));
        when(visitorService.getById(visitorId)).thenReturn(visitor());
        when(benefitService.getById(benefitId)).thenReturn(
                new BenefitResponse(benefitId, "Medical Expenses", new BigDecimal("5000"), Instant.now(), Instant.now()));
        when(icd11CodeService.getById(diagnosisId)).thenReturn(
                new Icd11CodeResponse(diagnosisId, "NOINJ", "No Injury", Instant.now(), Instant.now()));
    }

    @Test
    void sendsMappedClaimThenMarksItSent() {
        stubHappyPath();
        when(invoiceService.listByClaimId(claimId)).thenReturn(List.of(
                invoice("INV030212983", LocalDate.of(2026, 11, 7), "100000.00", LocalDate.of(2026, 11, 6))));

        MinetClaimSyncResponse response = service.syncClaim(claimId);

        ArgumentCaptor<MinetClaimRequest> captor = ArgumentCaptor.forClass(MinetClaimRequest.class);
        verify(minetClient).sendClaim(captor.capture());
        MinetClaimRequest sent = captor.getValue();
        assertThat(sent.providerName()).isEqualTo("AGA KHAN HOSPITAL NAIROBI");
        assertThat(sent.providerId()).isEqualTo("MSP-1001");
        assertThat(sent.benefit()).isEqualTo("Medical Expenses");
        assertThat(sent.passportNumber()).isEqualTo("7777");
        assertThat(sent.claimDate()).isEqualTo("2026-11-06");
        assertThat(sent.visitType()).isEqualTo("Outpatient");
        assertThat(sent.diagnosisCode()).isEqualTo("NOINJ");
        assertThat(sent.diagnosisDescription()).isEqualTo("No Injury");
        assertThat(sent.visitDate()).isEqualTo("2026-11-06");
        assertThat(sent.amountClaimed()).isEqualByComparingTo("100000.00");
        assertThat(sent.invoices()).hasSize(1);
        assertThat(sent.invoices().get(0).invoiceNumber()).isEqualTo("INV030212983");
        assertThat(sent.invoices().get(0).invoiceDate()).isEqualTo("2026-11-07");
        assertThat(sent.invoices().get(0).invoiceAmount()).isEqualByComparingTo("100000.00");
        assertThat(sent.invoices().get(0).service()).isEqualTo("Grouped Service");
        assertThat(sent.invoices().get(0).department()).isEqualTo("Grouped Department");
        assertThat(sent.claimAmountUsd()).isEqualByComparingTo("769.88");
        assertThat(sent.exchangeRate()).isEqualByComparingTo("0.008");
        assertThat(sent.inboundClaimReference()).isEqualTo(claimId.toString());
        verify(claimService).markSentToMinet(claimId);
        assertThat(response).isEqualTo(new MinetClaimSyncResponse(claimId, true));
    }

    @Test
    void sumsSeveralInvoicesAndFallsBackToClaimDateWithoutItems() {
        stubHappyPath();
        when(invoiceService.listByClaimId(claimId)).thenReturn(List.of(
                invoice("INV-1", null, "60000.00", null), invoice("INV-2", null, "40000.50", null)));

        service.syncClaim(claimId);

        ArgumentCaptor<MinetClaimRequest> captor = ArgumentCaptor.forClass(MinetClaimRequest.class);
        verify(minetClient).sendClaim(captor.capture());
        assertThat(captor.getValue().visitDate()).isEqualTo("2026-11-06");
        assertThat(captor.getValue().invoices()).extracting(MinetClaimRequest.Invoice::invoiceNumber)
                .containsExactly("INV-1", "INV-2");
        assertThat(captor.getValue().invoices().get(1).invoiceAmount()).isEqualByComparingTo("40000.50");
        assertThat(captor.getValue().invoices().get(0).invoiceDate()).isEqualTo("2026-11-06");
    }

    @Test
    void providerWithoutMinetIdIsRejectedAndNothingIsSent() {
        when(claimService.getEntityById(claimId)).thenReturn(claim());
        when(serviceProviderService.getById(providerId)).thenReturn(provider(" "));

        assertThatThrownBy(() -> service.syncClaim(claimId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Minet service provider id");
        verify(minetClient, never()).sendClaim(any());
        verify(claimService, never()).markSentToMinet(any());
    }

    @Test
    void claimWithoutServiceProviderIsRejected() {
        Claim claim = claim();
        claim.setServiceProviderId(null);
        when(claimService.getEntityById(claimId)).thenReturn(claim);

        assertThatThrownBy(() -> service.syncClaim(claimId)).isInstanceOf(IllegalStateException.class);
        verify(minetClient, never()).sendClaim(any());
    }

    @Test
    void minetFailureLeavesFlagUntouched() {
        stubHappyPath();
        when(invoiceService.listByClaimId(claimId)).thenReturn(List.of());
        doThrow(new MinetClientException("boom")).when(minetClient).sendClaim(any());

        assertThatThrownBy(() -> service.syncClaim(claimId)).isInstanceOf(MinetClientException.class);
        verify(claimService, never()).markSentToMinet(any());
    }
}
