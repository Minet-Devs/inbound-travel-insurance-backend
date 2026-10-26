package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.AttachInvoiceRequest;
import com.travel.insurance.claim.dto.ClaimDecisionRequest;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.claim.dto.ConvertPreauthorizationRequest;
import com.travel.insurance.claim.dto.ClaimExportRow;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ClaimService claimService;
    private final ClaimExcelWriter claimExcelWriter;

    @PostMapping
    public ResponseEntity<ClaimResponse> create(@RequestBody ClaimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.create(request));
    }

    /** Claim + invoice built from an approved pre-authorization; the caller only supplies the invoice number. */
    @PostMapping("/from-preauthorization/{preauthorizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTACT_CENTER')")
    public ResponseEntity<ClaimResponse> createFromPreauthorization(
            @PathVariable UUID preauthorizationId, @Valid @RequestBody ConvertPreauthorizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(claimService.createFromPreauthorization(preauthorizationId, request));
    }

    @PutMapping("/{id}/invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_USER')")
    public ResponseEntity<ClaimResponse> attachInvoice(
            @PathVariable UUID id, @RequestBody AttachInvoiceRequest request) {
        return ResponseEntity.ok(claimService.attachInvoice(id, request));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) UUID insurerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        List<ClaimExportRow> rows = claimService.listForExport(status, insurerId, dateFrom, dateTo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, XLSX_CONTENT_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"claims.xlsx\"")
                .body(claimExcelWriter.write(rows));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(claimService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ClaimResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(claimService.list(pageable));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSURER_USER')")
    public ResponseEntity<ClaimResponse> decide(@PathVariable UUID id,
                                                @Valid @RequestBody ClaimDecisionRequest request) {
        return ResponseEntity.ok(claimService.decide(id, request));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSURER_USER')")
    public ResponseEntity<ClaimResponse> markPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(claimService.markPaid(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        claimService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
