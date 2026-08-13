package com.travel.insurance.invoice;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    @Override
    public InvoiceResponse create(InvoiceRequest request) {
        Invoice invoice = invoiceRepository.save(invoiceMapper.toEntity(request));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        return invoiceMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> list(UUID claimId, Pageable pageable) {
        Page<Invoice> page = claimId != null
                ? invoiceRepository.findAllByClaimId(claimId, pageable)
                : invoiceRepository.findAll(pageable);
        return page.map(invoiceMapper::toResponse);
    }

    @Override
    public InvoiceResponse update(UUID id, InvoiceRequest request) {
        Invoice invoice = getEntityById(id);
        invoiceMapper.updateEntity(invoice, request);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
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
}
