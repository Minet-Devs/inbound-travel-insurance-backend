package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.ClaimDecisionRequest;
import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody ClaimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.create(request));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'INSURER_USER')")
    public ResponseEntity<ClaimResponse> decide(@PathVariable UUID id,
                                                @Valid @RequestBody ClaimDecisionRequest request) {
        return ResponseEntity.ok(claimService.decide(id, request));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'INSURER_USER')")
    public ResponseEntity<ClaimResponse> markPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(claimService.markPaid(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        claimService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
