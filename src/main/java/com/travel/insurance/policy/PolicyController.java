package com.travel.insurance.policy;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.policy.dto.PolicyDetailResponse;
import com.travel.insurance.policy.dto.PolicyRequest;
import com.travel.insurance.policy.dto.PolicyResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final BenefitService benefitService;

    @PostMapping
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyDetailResponse> getById(@PathVariable UUID id) {
        PolicyResponse policy = policyService.getById(id);
        return ResponseEntity.ok(PolicyDetailResponse.of(policy, benefitService.listAllByPolicy(id)));
    }

    @GetMapping
    public ResponseEntity<Page<PolicyDetailResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(policyService.list(pageable).map(policy ->
                PolicyDetailResponse.of(policy, benefitService.listAllByPolicy(policy.id()))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
