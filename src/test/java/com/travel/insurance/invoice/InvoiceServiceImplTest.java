package com.travel.insurance.invoice;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.service.CurrencyConversionService;
import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.medicalservice.MedicalServiceService;
import com.travel.insurance.medicalservice.dto.MedicalServiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MedicalServiceService medicalServiceService;

    @Mock
    private CurrencyConversionService currencyConversionService;

    private final InvoiceMapper invoiceMapper = new InvoiceMapper();

    private InvoiceServiceImpl invoiceService;

    private UUID claimId;
    private UUID medicalServiceId;
    private InvoiceRequest request;

    private static final BigDecimal KES_TO_USD = new BigDecimal("0.0077");

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceServiceImpl(
                invoiceRepository, invoiceMapper, medicalServiceService, currencyConversionService);
        claimId = UUID.randomUUID();
        medicalServiceId = UUID.randomUUID();
        request = new InvoiceRequest(
                claimId,
                "INV-2026-001",
                LocalDate.of(2026, 8, 1),
                "KES",
                new BigDecimal("25000.00"),
                List.of(new InvoiceItemRequest(
                        null, "In-patient care", new BigDecimal("1"), new BigDecimal("25000.00"),
                        new BigDecimal("25000.00"), LocalDate.of(2026, 8, 1))));
    }

    private MedicalServiceResponse medicalServiceResponse() {
        return new MedicalServiceResponse(medicalServiceId, "In-patient Care", UUID.randomUUID(),
                "Inpatient", java.time.Instant.now(), java.time.Instant.now());
    }

    @Test
    void createSavesInvoiceWithClaimAndLineItems() {
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = invoiceService.create(request);

        assertThat(response.claimId()).isEqualTo(claimId);
        assertThat(response.invoiceNumber()).isEqualTo("INV-2026-001");
        assertThat(response.totalAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.invoiceItems()).hasSize(1);
        assertThat(response.invoiceItems().getFirst().description()).isEqualTo("In-patient care");
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createConvertsRawAmountsToUsdBaseAtSavedRate() {
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = invoiceService.create(request);

        assertThat(response.exchangeRate()).isEqualByComparingTo("0.0077");
        assertThat(response.baseCurrency()).isEqualTo("USD");
        assertThat(response.baseTotalAmount()).isEqualByComparingTo("192.50");
        assertThat(response.fxRateDate()).isNotNull();
        assertThat(response.invoiceItems().getFirst().baseUnitPrice()).isEqualByComparingTo("192.50");
        assertThat(response.invoiceItems().getFirst().baseAmount()).isEqualByComparingTo("192.50");
    }

    @Test
    void createAllowsInvoiceWithoutLineItems() {
        when(currencyConversionService.getExchangeRate("USD", "USD")).thenReturn(BigDecimal.ONE);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceRequest noItems = new InvoiceRequest(
                claimId, "INV-2026-002", LocalDate.of(2026, 8, 2), "USD",
                new BigDecimal("1000.00"), List.of());

        InvoiceResponse response = invoiceService.create(noItems);

        assertThat(response.invoiceItems()).isEmpty();
        assertThat(response.exchangeRate()).isEqualByComparingTo("1");
        assertThat(response.baseTotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.baseCurrency()).isEqualTo("USD");
    }

    @Test
    void createSavesInvoiceWithMedicalServiceAndResolvesName() {
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(medicalServiceService.getById(medicalServiceId)).thenReturn(medicalServiceResponse());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceRequest withMedicalService = new InvoiceRequest(
                claimId, "INV-2026-003", LocalDate.of(2026, 8, 2), "KES",
                new BigDecimal("12000.00"), List.of(new InvoiceItemRequest(
                        medicalServiceId, "In-patient care", new BigDecimal("1"), new BigDecimal("12000.00"),
                        new BigDecimal("12000.00"), LocalDate.of(2026, 8, 2))));

        InvoiceResponse response = invoiceService.create(withMedicalService);

        assertThat(response.invoiceItems()).hasSize(1);
        assertThat(response.invoiceItems().getFirst().medicalServiceId()).isEqualTo(medicalServiceId);
        assertThat(response.invoiceItems().getFirst().medicalServiceName()).isEqualTo("In-patient Care");
        assertThat(response.baseTotalAmount()).isEqualByComparingTo("92.40");
    }

    @Test
    void createWithoutMedicalServiceReturnsNullName() {
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = invoiceService.create(request);

        assertThat(response.invoiceItems()).hasSize(1);
        assertThat(response.invoiceItems().getFirst().medicalServiceId()).isNull();
        assertThat(response.invoiceItems().getFirst().medicalServiceName()).isNull();
    }

    @Test
    void createRejectsUnknownMedicalService() {
        when(medicalServiceService.getById(medicalServiceId))
                .thenThrow(new ResourceNotFoundException("MedicalService", medicalServiceId));

        InvoiceRequest withMedicalService = new InvoiceRequest(
                claimId, "INV-2026-004", LocalDate.of(2026, 8, 2), "KES",
                new BigDecimal("5000.00"), List.of(new InvoiceItemRequest(
                        medicalServiceId, "Care", new BigDecimal("1"), new BigDecimal("5000.00"),
                        new BigDecimal("5000.00"), LocalDate.of(2026, 8, 2))));

        assertThatThrownBy(() -> invoiceService.create(withMedicalService))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsInvoiceWithItems() {
        Invoice invoice = invoiceMapper.toEntity(request);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getById(invoice.getId());

        assertThat(response.invoiceNumber()).isEqualTo("INV-2026-001");
        assertThat(response.invoiceItems()).hasSize(1);
    }

    @Test
    void getByIdThrowsWhenUnknown() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listByClaimIdReturnsOnlyThatClaimsInvoices() {
        Invoice invoice = invoiceMapper.toEntity(request);
        Pageable pageable = PageRequest.of(0, 10);
        when(invoiceRepository.findAllByClaimId(claimId, pageable))
                .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));

        Page<InvoiceResponse> page = invoiceService.list(claimId, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().claimId()).isEqualTo(claimId);
    }

    @Test
    void listWithoutClaimIdReturnsAll() {
        Invoice invoice = invoiceMapper.toEntity(request);
        Pageable pageable = PageRequest.of(0, 10);
        when(invoiceRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));

        Page<InvoiceResponse> page = invoiceService.list(null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void updateReplacesHeaderAndLineItems() {
        UUID id = UUID.randomUUID();
        Invoice existing = invoiceMapper.toEntity(request);
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(existing));
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(KES_TO_USD);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceRequest updated = new InvoiceRequest(
                claimId, "INV-2026-001-R", LocalDate.of(2026, 8, 3), "KES",
                new BigDecimal("30000.00"),
                List.of(new InvoiceItemRequest(
                        null, "Surgery", new BigDecimal("1"), new BigDecimal("30000.00"),
                        new BigDecimal("30000.00"), LocalDate.of(2026, 8, 3))));

        InvoiceResponse response = invoiceService.update(id, updated);

        assertThat(response.invoiceNumber()).isEqualTo("INV-2026-001-R");
        assertThat(response.totalAmount()).isEqualByComparingTo("30000.00");
        assertThat(response.invoiceItems()).hasSize(1);
        assertThat(response.invoiceItems().getFirst().description()).isEqualTo("Surgery");
        assertThat(response.exchangeRate()).isEqualByComparingTo("0.0077");
        assertThat(response.baseTotalAmount()).isEqualByComparingTo("231.00");
        assertThat(response.invoiceItems().getFirst().baseAmount()).isEqualByComparingTo("231.00");
    }

    @Test
    void updateThrowsWhenUnknown() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesInvoice() {
        UUID id = UUID.randomUUID();
        Invoice invoice = invoiceMapper.toEntity(request);
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        invoiceService.delete(id);

        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void getEntityByIdReturnsEntity() {
        UUID id = UUID.randomUUID();
        Invoice invoice = invoiceMapper.toEntity(request);
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThat(invoiceService.getEntityById(id)).isSameAs(invoice);
    }
}
