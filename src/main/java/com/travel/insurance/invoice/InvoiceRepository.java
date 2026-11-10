package com.travel.insurance.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findAllByClaimId(UUID claimId, Pageable pageable);

    List<Invoice> findAllByClaimId(UUID claimId);

    @Query("SELECT DISTINCT i.claimId FROM Invoice i "
            + "WHERE LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :invoiceNumber, '%'))")
    List<UUID> findClaimIdsByInvoiceNumberContaining(@Param("invoiceNumber") String invoiceNumber);
}
