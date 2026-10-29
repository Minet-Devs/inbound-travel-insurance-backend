package com.travel.insurance.preauthorization;

import com.travel.insurance.preauthorization.dto.InsurerPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.PreauthorizationDashboardSummary;
import com.travel.insurance.preauthorization.dto.ServiceProviderPreauthorizationDistribution;
import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadResponse;
import com.travel.insurance.preauthorization.upload.PreauthorizationUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preauthorizations")
@RequiredArgsConstructor
public class PreauthorizationController {

    private final PreauthorizationService preauthorizationService;
    private final PreauthorizationUploadService preauthorizationUploadService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_USER', 'CONTACT_CENTER')")
    public ResponseEntity<PreauthorizationResponse> create(
            @Valid @RequestBody PreauthorizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preauthorizationService.create(request));
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTACT_CENTER')")
    public ResponseEntity<PreauthorizationUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(preauthorizationUploadService.upload(file));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<PreauthorizationDashboardSummary> dashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(preauthorizationService.dashboardSummary(dateFrom, dateTo));
    }

    @GetMapping("/dashboard/by-insurer")
    public ResponseEntity<List<InsurerPreauthorizationDistribution>> distributionByInsurer(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(preauthorizationService.distributionByInsurer(dateFrom, dateTo));
    }

    @GetMapping("/dashboard/top-service-providers")
    public ResponseEntity<List<ServiceProviderPreauthorizationDistribution>> topServiceProviders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(preauthorizationService.topServiceProviders(dateFrom, dateTo, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PreauthorizationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(preauthorizationService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PreauthorizationResponse>> list(
            @RequestParam(required = false) UUID insurerId, Pageable pageable) {
        return ResponseEntity.ok(preauthorizationService.list(insurerId, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_USER', 'CONTACT_CENTER')")
    public ResponseEntity<PreauthorizationResponse> update(
            @PathVariable UUID id, @Valid @RequestBody PreauthorizationRequest request) {
        return ResponseEntity.ok(preauthorizationService.update(id, request));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSURER_USER', 'CONTACT_CENTER')")
    public ResponseEntity<PreauthorizationResponse> decide(
            @PathVariable UUID id, @Valid @RequestBody PreauthorizationDecisionRequest request) {
        return ResponseEntity.ok(preauthorizationService.decide(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        preauthorizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
