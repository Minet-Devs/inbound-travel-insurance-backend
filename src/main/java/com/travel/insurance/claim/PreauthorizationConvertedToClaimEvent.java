package com.travel.insurance.claim;

import java.util.UUID;

/** Published after a pre-authorization has been converted to a claim (the claim and its invoice are saved). */
public record PreauthorizationConvertedToClaimEvent(UUID claimId, UUID preauthorizationId) {
}
