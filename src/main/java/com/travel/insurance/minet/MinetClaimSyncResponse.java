package com.travel.insurance.minet;

import java.util.UUID;

public record MinetClaimSyncResponse(UUID claimId, boolean sentToMinet) {
}
