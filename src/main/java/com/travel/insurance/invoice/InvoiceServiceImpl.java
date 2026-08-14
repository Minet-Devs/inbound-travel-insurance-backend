package com.travel.insurance.invoice;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.service.CurrencyConversionService;
import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.medicalservice.MedicalServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private static final String DEFAULT_CURRENCY = "KES";
    private static final String BASE_CURRENCY = "USD";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final MedicalServiceService medicalServiceService;
    private final CurrencyConversionService currencyConversionService;

    @Override
    public InvoiceResponse create(InvoiceRequest request) {
        validateReferences(request);
        BigDecimal rate = currencyConversionService.getExchangeRate(normalizedCurrency(request.currency()), BASE_CURRENCY);
        Invoice invoice = invoiceMapper.toEntity(request);
        invoice.setCurrency(normalizedCurrency(request.currency()));
        applyBaseConversion(invoice, rate);
        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        return toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> list(UUID claimId, Pageable pageable) {
        Page<Invoice> page = claimId != null
                ? invoiceRepository.findAllByClaimId(claimId, pageable)
                : invoiceRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Override
    public InvoiceResponse update(UUID id, InvoiceRequest request) {
        Invoice invoice = getEntityById(id);
        validateReferences(request);
        invoiceMapper.updateEntity(invoice, request);
        BigDecimal rate = currencyConversionService.getExchangeRate(normalizedCurrency(request.currency()), BASE_CURRENCY);
        invoice.setCurrency(normalizedCurrency(request.currency()));
        applyBaseConversion(invoice, rate);
        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public void delete(UUID id) {
        invoiceRepository.delete(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getEntityById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    private void validateReferences(InvoiceRequest request) {
        if (request.invoiceItems() != null) {
            for (InvoiceItemRequest item : request.invoiceItems()) {
                if (item.medicalServiceId() != null) {
                    medicalServiceService.getById(item.medicalServiceId());
                }
            }
        }
    }

    private static String normalizedCurrency(String currency) {
        return currency == null || currency.isBlank() ? DEFAULT_CURRENCY : currency;
    }

    private static void applyBaseConversion(Invoice invoice, BigDecimal rate) {
        invoice.getInvoiceItems().forEach(item -> {
            item.setBaseUnitPrice(toBase(item.getUnitPrice(), rate));
            item.setBaseAmount(toBase(item.getAmount(), rate));
        });
        invoice.setExchangeRate(rate);
        invoice.setBaseCurrency(BASE_CURRENCY);
        invoice.setBaseTotalAmount(toBase(invoice.getTotalAmount(), rate));
        invoice.setFxRateDate(LocalDateTime.now());
    }

    private static BigDecimal toBase(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return invoiceMapper.toResponse(invoice, medicalServiceId -> medicalServiceService.getById(medicalServiceId).name());
    }
}
