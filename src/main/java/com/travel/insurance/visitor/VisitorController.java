package com.travel.insurance.visitor;

import com.travel.insurance.visitor.dto.VisitorDetailResponse;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import com.travel.insurance.visitorbenefit.VisitorBenefitService;
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
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;
    private final VisitorBenefitService visitorBenefitService;

    @PostMapping
    public ResponseEntity<VisitorResponse> create(@Valid @RequestBody VisitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitorService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisitorDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(toDetail(visitorService.getById(id)));
    }

    @GetMapping("/by-passport")
    public ResponseEntity<VisitorDetailResponse> getByPassportNumber(
            @RequestParam String passportNumber) {
        return ResponseEntity.ok(toDetail(visitorService.getByPassportNumber(passportNumber)));
    }

    @GetMapping("/by-policy")
    public ResponseEntity<List<VisitorDetailResponse>> listByPolicyId(@RequestParam UUID policyId) {
        return ResponseEntity.ok(visitorService.listByPolicyId(policyId).stream()
                .map(this::toDetail)
                .toList());
    }

    @GetMapping
    public ResponseEntity<Page<VisitorResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(visitorService.list(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VisitorResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody VisitorRequest request) {
        return ResponseEntity.ok(visitorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        visitorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private VisitorDetailResponse toDetail(VisitorResponse visitor) {
        return VisitorDetailResponse.of(
                visitor, visitorBenefitService.listAllByVisitor(visitor.id()));
    }
}
