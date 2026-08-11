package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.benefit.dto.BenefitTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Benefits are read-only over the API: every policy inherits the fixed
 * {@link BenefitType} catalog with mandated limits when it is created, so there
 * are no create/update/delete endpoints.
 */
@RestController
@RequestMapping("/api/v1/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;

    @GetMapping("/types")
    public ResponseEntity<List<BenefitTypeResponse>> listBenefitTypes() {
        return ResponseEntity.ok(benefitService.listBenefitTypes());
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
}
