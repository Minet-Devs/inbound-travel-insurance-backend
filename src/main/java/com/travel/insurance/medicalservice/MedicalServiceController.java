package com.travel.insurance.medicalservice;

import com.travel.insurance.medicalservice.dto.MedicalServiceImportResult;
import com.travel.insurance.medicalservice.dto.MedicalServiceRequest;
import com.travel.insurance.medicalservice.dto.MedicalServiceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Manages the service catalog: each service belongs to a department, referenced
 * by the department's public id only (no embedded relation). Supports admin bulk
 * import from the service/department master-list Excel workbook.
 */
@RestController
@RequestMapping("/api/v1/medical-services")
@RequiredArgsConstructor
public class MedicalServiceController {

    private final MedicalServiceService medicalServiceService;

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalServiceImportResult> importServices(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(medicalServiceService.importFromExcel(file));
    }

    @PostMapping
    public ResponseEntity<MedicalServiceResponse> create(@Valid @RequestBody MedicalServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalServiceService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalServiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(medicalServiceService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<MedicalServiceResponse>> list(
            @RequestParam(required = false) UUID departmentId, Pageable pageable) {
        return ResponseEntity.ok(medicalServiceService.list(departmentId, pageable));
    }

    @GetMapping("/by-department/{departmentId}")
    public ResponseEntity<List<MedicalServiceResponse>> listAllByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(medicalServiceService.listAllByDepartment(departmentId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalServiceResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody MedicalServiceRequest request) {
        return ResponseEntity.ok(medicalServiceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicalServiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}