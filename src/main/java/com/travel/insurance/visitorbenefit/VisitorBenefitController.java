package com.travel.insurance.visitorbenefit;

import com.travel.insurance.visitorbenefit.dto.VisitorBenefitRequest;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visitor-benefits")
@RequiredArgsConstructor
public class VisitorBenefitController {

    private final VisitorBenefitService visitorBenefitService;

    @PostMapping
    public ResponseEntity<VisitorBenefitResponse> create(
            @Valid @RequestBody VisitorBenefitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitorBenefitService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisitorBenefitResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(visitorBenefitService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<VisitorBenefitResponse>> list(
            @RequestParam(required = false) UUID visitorId, Pageable pageable) {
        return ResponseEntity.ok(visitorBenefitService.list(visitorId, pageable));
    }

    @GetMapping("/by-visitor/{visitorId}")
    public ResponseEntity<List<VisitorBenefitResponse>> listAllByVisitor(
            @PathVariable UUID visitorId) {
        return ResponseEntity.ok(visitorBenefitService.listAllByVisitor(visitorId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VisitorBenefitResponse> update(
            @PathVariable UUID id, @Valid @RequestBody VisitorBenefitRequest request) {
        return ResponseEntity.ok(visitorBenefitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        visitorBenefitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
