package com.travel.insurance.minet;

import com.travel.insurance.visitor.VisitorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
public class MinetController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final MinetVisitorSyncService minetVisitorSyncService;
    private final MinetVisitorExcelWriter minetVisitorExcelWriter;

    @PostMapping("/minet-sync/{passportNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public MinetSyncResponse sync(@PathVariable String passportNumber) {
        return minetVisitorSyncService.syncByPassportNumber(passportNumber);
    }

    @GetMapping("/export/minet-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) UUID insurerId,
            @RequestParam(required = false) VisitorStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, XLSX_CONTENT_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"minet-visitors.xlsx\"")
                .body(minetVisitorExcelWriter.write(
                        minetVisitorSyncService.listForExport(insurerId, status, dateFrom, dateTo)));
    }
}
