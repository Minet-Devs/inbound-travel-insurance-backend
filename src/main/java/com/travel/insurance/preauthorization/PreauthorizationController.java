package com.travel.insurance.preauthorization;

import com.travel.insurance.preauthorization.dto.PreauthorizationDecisionRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preauthorizations")
@RequiredArgsConstructor
public class PreauthorizationController {

    private final PreauthorizationService preauthorizationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'PROVIDER_USER')")
    public ResponseEntity<PreauthorizationResponse> create(
            @Valid @RequestBody PreauthorizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preauthorizationService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PreauthorizationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(preauthorizationService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PreauthorizationResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(preauthorizationService.list(pageable));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'INSURER_USER')")
    public ResponseEntity<PreauthorizationResponse> decide(
            @PathVariable UUID id, @Valid @RequestBody PreauthorizationDecisionRequest request) {
        return ResponseEntity.ok(preauthorizationService.decide(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        preauthorizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
