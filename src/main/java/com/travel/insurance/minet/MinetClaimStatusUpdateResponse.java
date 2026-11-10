package com.travel.insurance.minet;

import java.util.UUID;

/** {@code applied} is false when the event was a duplicate or older than the last applied update. */
public record MinetClaimStatusUpdateResponse(UUID claimId, boolean applied) {
}
