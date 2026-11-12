package com.travel.insurance.invoice;

import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.service.CurrencyConversionService;
import com.travel.insurance.invoice.dto.InvoiceItemRequest;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.medicalservice.MedicalServiceService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private static final String DEFAULT_CURRENCY = "KES";
    private static final String BASE_CURRENCY = "USD";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final MedicalServiceService medicalServiceService;
    private final CurrencyConversionService currencyConversionService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ClaimService claimService;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, InvoiceMapper invoiceMapper,
                              MedicalServiceService medicalServiceService,
                              CurrencyConversionService currencyConversionService,
                              ApplicationEventPublisher applicationEventPublisher,
                              @Lazy ClaimService claimService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.medicalServiceService = medicalServiceService;
        this.currencyConversionService = currencyConversionService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.claimService = claimService;
    }

    @Override
    public InvoiceResponse create(InvoiceRequest request) {
        validateReferences(request);
        rejectDuplicate(request);
        BigDecimal rate = currencyConversionService.getExchangeRate(normalizedCurrency(request.currency()), BASE_CURRENCY);
        Invoice invoice = invoiceMapper.toEntity(request);
        invoice.setCurrency(normalizedCurrency(request.currency()));
        applyBaseConversion(invoice, rate);
        Invoice saved = invoiceRepository.save(invoice);
        publishChanged(saved.getClaimId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        return toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return invoiceRepository.findAllById(ids).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findClaimIdsByInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            return List.of();
        }
        return invoiceRepository.findClaimIdsByInvoiceNumberContaining(invoiceNumber.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> listByClaimId(UUID claimId) {
        return invoiceRepository.findAllByClaimId(claimId).stream().map(this::toResponse).toList();
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
        UUID previousClaimId = invoice.getClaimId();
        validateReferences(request);
        invoiceMapper.updateEntity(invoice, request);
        BigDecimal rate = currencyConversionService.getExchangeRate(normalizedCurrency(request.currency()), BASE_CURRENCY);
        invoice.setCurrency(normalizedCurrency(request.currency()));
        applyBaseConversion(invoice, rate);
        Invoice saved = invoiceRepository.save(invoice);
        publishChanged(previousClaimId);
        if (!Objects.equals(previousClaimId, saved.getClaimId())) {
            publishChanged(saved.getClaimId());
        }
        return toResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        Invoice invoice = getEntityById(id);
        invoiceRepository.delete(invoice);
        publishChanged(invoice.getClaimId());
    }

    /** Lets the owning claim re-total its amount (or veto the change when it is locked). */
    private void publishChanged(UUID claimId) {
        if (claimId != null) {
            applicationEventPublisher.publishEvent(new InvoiceChangedEvent(claimId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getEntityById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    @Override
    public void attachToClaim(UUID invoiceId, UUID claimId) {
        Invoice invoice = getEntityById(invoiceId);
        invoice.setClaimId(claimId);
        invoiceRepository.save(invoice);
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

    /**
     * An invoice is a duplicate when another one has the same number and total on a claim with the same
     * provider, benefit and visitor (a visitor maps 1:1 to a passport number). Skipped without an invoice number.
     */
    private void rejectDuplicate(InvoiceRequest request) {
        if (request.invoiceNumber() == null || request.invoiceNumber().isBlank()) {
            return;
        }
        List<UUID> comparableClaimIds = claimService.findIdsWithSameProviderBenefitAndVisitor(request.claimId());
        if (!comparableClaimIds.isEmpty() && invoiceRepository.existsDuplicate(
                request.invoiceNumber().trim(), request.totalAmount(), comparableClaimIds)) {
            throw new IllegalStateException("Duplicate invoice: invoice number " + request.invoiceNumber().trim()
                    + " with the same amount already exists for this provider, benefit and passport");
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
