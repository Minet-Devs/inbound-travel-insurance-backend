package com.travel.insurance.memberstatement;

import com.travel.insurance.memberstatement.dto.MemberStatementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/member-statements")
@RequiredArgsConstructor
public class MemberStatementController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final MemberStatementService memberStatementService;

    @GetMapping
    public ResponseEntity<MemberStatementResponse> getStatement(@RequestParam String passportNumber) {
        return ResponseEntity.ok(memberStatementService.getStatement(passportNumber));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String passportNumber,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam MemberStatementExportType exportType) {
        byte[] body = memberStatementService.export(passportNumber, fromDate, toDate, exportType);
        String extension = exportType == MemberStatementExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == MemberStatementExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE : XLSX_CONTENT_TYPE;
        String filename = "member-statement-" + passportNumber + "." + extension;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}