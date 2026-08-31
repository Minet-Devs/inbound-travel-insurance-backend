package com.travel.insurance.premiumreceipt;

import com.travel.insurance.premiumreceipt.dto.PremiumReceiptPatchRequest;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/premium-receipts")
@RequiredArgsConstructor
public class PremiumReceiptController {

    private final PremiumReceiptService premiumReceiptService;

    @GetMapping
    public ResponseEntity<PremiumReceiptResponse> get() {
        return ResponseEntity.ok(premiumReceiptService.get());
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PremiumReceiptResponse> patch(@Valid @RequestBody PremiumReceiptPatchRequest request) {
        return ResponseEntity.ok(premiumReceiptService.patch(request));
    }
}
