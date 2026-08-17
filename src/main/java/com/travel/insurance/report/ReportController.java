package com.travel.insurance.report;

import com.travel.insurance.report.dto.ClaimReceiptResponse;
import com.travel.insurance.report.dto.ProviderClaimReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/claims/{claimId}/pdf")
    public ResponseEntity<byte[]> claimReceiptPdf(@PathVariable UUID claimId) {
        byte[] pdf = reportService.generateClaimReceiptPdf(claimId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=claim-receipt-" + claimId + ".pdf")
                .body(pdf);
    }

    @GetMapping("/claims/{claimId}")
    public ResponseEntity<ClaimReceiptResponse> claimReceipt(@PathVariable UUID claimId) {
        return ResponseEntity.ok(reportService.getClaimReceipt(claimId));
    }

    @GetMapping("/service-providers/{providerId}/claims")
    public ResponseEntity<ProviderClaimReportResponse> providerReport(
            @PathVariable UUID providerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return ResponseEntity.ok(reportService.getProviderReport(providerId, status, dateFrom, dateTo, pageable));
    }

    @GetMapping("/service-providers/{providerId}/claims/pdf")
    public ResponseEntity<byte[]> providerReportPdf(
            @PathVariable UUID providerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] pdf = reportService.generateProviderReportPdf(providerId, status, dateFrom, dateTo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=provider-claims-report.pdf")
                .body(pdf);
    }

    @GetMapping("/service-providers/{providerId}/claims/excel")
    public ResponseEntity<byte[]> providerReportExcel(
            @PathVariable UUID providerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] excel = reportService.generateProviderReportExcel(providerId, status, dateFrom, dateTo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=provider-claims-report.xlsx")
                .body(excel);
    }
}
