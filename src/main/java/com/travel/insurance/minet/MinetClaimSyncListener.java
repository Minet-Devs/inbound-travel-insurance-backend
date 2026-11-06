package com.travel.insurance.minet;

import com.travel.insurance.claim.PreauthorizationConvertedToClaimEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends every claim created from a pre-authorization to Minet once the converting transaction has committed.
 * A failure is logged and never affects the claim that was already saved; the claim stays
 * {@code sent_to_minet = false} and can be re-sent through the resend API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinetClaimSyncListener {

    private final MinetClaimSyncService minetClaimSyncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConverted(PreauthorizationConvertedToClaimEvent event) {
        try {
            minetClaimSyncService.syncClaim(event.claimId());
        } catch (IllegalStateException ex) {
            log.warn("Claim {} not sent to Minet: {}", event.claimId(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to send claim {} to Minet: {}", event.claimId(), ex.getMessage(), ex);
        }
    }
}
