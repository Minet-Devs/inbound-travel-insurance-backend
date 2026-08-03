package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;

    @PostMapping
    public ResponseEntity<List<BenefitResponse>> create(
            @RequestBody @NotEmpty List<@Valid BenefitRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(benefitService.create(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BenefitResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(benefitService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<BenefitResponse>> list(@RequestParam(required = false) UUID policyId,
                                                      Pageable pageable) {
        return ResponseEntity.ok(benefitService.list(policyId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BenefitResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody BenefitRequest request) {
        return ResponseEntity.ok(benefitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        benefitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
