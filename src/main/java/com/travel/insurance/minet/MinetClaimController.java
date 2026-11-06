package com.travel.insurance.minet;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class MinetClaimController {

    private final MinetClaimSyncService minetClaimSyncService;

    /** Sends (or re-sends) the claim to Minet and sets {@code sent_to_minet}. */
    @PostMapping("/{id}/minet-sync")
    @PreAuthorize("hasRole('ADMIN')")
    public MinetClaimSyncResponse sync(@PathVariable UUID id) {
        return minetClaimSyncService.syncClaim(id);
    }
}
