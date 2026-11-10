package com.travel.insurance.invoice;

import com.travel.insurance.invoice.dto.InvoiceRequest;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    InvoiceResponse create(InvoiceRequest request);

    InvoiceResponse getById(UUID id);

    List<InvoiceResponse> getByIds(Collection<UUID> ids);

    List<InvoiceResponse> listByClaimId(UUID claimId);

    /** Distinct claim ids of invoices whose number contains {@code invoiceNumber}, ignoring case. */
    List<UUID> findClaimIdsByInvoiceNumber(String invoiceNumber);

    Page<InvoiceResponse> list(UUID claimId, Pageable pageable);

    InvoiceResponse update(UUID id, InvoiceRequest request);

    void delete(UUID id);

    Invoice getEntityById(UUID id);

    void attachToClaim(UUID invoiceId, UUID claimId);
}
