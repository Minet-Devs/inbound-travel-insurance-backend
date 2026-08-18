package com.travel.insurance.insurer;

import com.travel.insurance.insurer.dto.InsurerDetailResponse;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
import com.travel.insurance.policy.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insurers")
@RequiredArgsConstructor
public class InsurerController {

    private final InsurerService insurerService;
    private final PolicyService policyService;

    @PostMapping
    public ResponseEntity<InsurerResponse> create(@Valid @RequestBody InsurerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insurerService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsurerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(insurerService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<InsurerResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(insurerService.list(pageable));
    }

    @GetMapping("/detailed")
    public ResponseEntity<List<InsurerDetailResponse>> listDetailed() {
        List<InsurerDetailResponse> insurers = insurerService.listAll().stream()
                .map(insurer -> InsurerDetailResponse.of(insurer, policyService.listByInsurerId(insurer.id())))
                .toList();
        return ResponseEntity.ok(insurers);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsurerResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody InsurerRequest request) {
        return ResponseEntity.ok(insurerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        insurerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
