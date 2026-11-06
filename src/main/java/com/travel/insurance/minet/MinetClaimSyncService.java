package com.travel.insurance.minet;

import java.util.UUID;

public interface MinetClaimSyncService {

    /**
     * Sends the claim to Minet's inbound claim API and, only once Minet accepted it, sets
     * {@code claims.sent_to_minet = true}. Sends again if the flag was already set. Throws
     * {@code ResourceNotFoundException} for an unknown claim, {@code IllegalStateException} when the claim has no
     * service provider or the provider has no Minet service provider id, and {@link MinetClientException}
     * when Minet fails.
     */
    MinetClaimSyncResponse syncClaim(UUID claimId);
}
