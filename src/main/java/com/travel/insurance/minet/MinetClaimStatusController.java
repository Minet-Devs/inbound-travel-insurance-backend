package com.travel.insurance.minet;

import com.travel.insurance.claim.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/minet")
@RequiredArgsConstructor
public class MinetClaimStatusController {

    private final ClaimService claimService;

    /**
     * Receives Minet's claim status events. The claim is found by {@code inboundClaimReference}, which is the
     * claim id we sent. Authenticated like the rest of the API (Bearer JWT).
     */
    @PostMapping("/claim-status")
    @PreAuthorize("hasRole('ADMIN')")
    public MinetClaimStatusUpdateResponse claimStatus(@RequestBody MinetClaimStatusUpdateRequest request) {
        UUID claimId = parseReference(request.inboundClaimReference());
        boolean applied = claimService.applyMinetStatusUpdate(
                claimId, request.eventId(), request.status(), request.changedAt(), request.comments());
        return new MinetClaimStatusUpdateResponse(claimId, applied);
    }

    private static UUID parseReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("inboundClaimReference is required");
        }
        try {
            return UUID.fromString(reference.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("inboundClaimReference is not a valid claim id: " + reference);
        }
    }
}
