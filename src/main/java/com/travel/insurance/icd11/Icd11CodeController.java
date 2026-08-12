package com.travel.insurance.icd11;

import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.icd11.dto.Icd11ImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * ICD-11 diagnosis code catalog: admin bulk import from an Excel workbook, plus
 * search and single-code lookup for downstream diagnosis selection.
 */
@RestController
@RequestMapping("/api/v1/icd11-codes")
@RequiredArgsConstructor
public class Icd11CodeController {

    private final Icd11CodeService icd11CodeService;

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Icd11ImportResult> importCodes(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(icd11CodeService.importFromExcel(file));
    }

    @GetMapping
    public ResponseEntity<Page<Icd11CodeResponse>> search(
            @RequestParam(required = false) String query, Pageable pageable) {
        return ResponseEntity.ok(icd11CodeService.search(query, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Icd11CodeResponse>> searchByTitle(
            @RequestParam String title, Pageable pageable) {
        return ResponseEntity.ok(icd11CodeService.searchByTitle(title, pageable));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Icd11CodeResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(icd11CodeService.getByCode(code));
    }
}
