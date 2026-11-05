package com.travel.insurance.minet;

import java.util.UUID;

public record MinetSyncResponse(UUID visitorId, boolean sentToMinet) {
}
