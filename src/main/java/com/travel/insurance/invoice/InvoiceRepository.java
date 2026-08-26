package com.travel.insurance.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findAllByClaimId(UUID claimId, Pageable pageable);

    List<Invoice> findAllByClaimId(UUID claimId);
}
