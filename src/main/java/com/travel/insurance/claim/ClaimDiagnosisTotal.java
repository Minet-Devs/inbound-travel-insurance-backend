package com.travel.insurance.claim;

import java.util.UUID;

/** Number of claims carrying one diagnosis, optionally for one visitor (only the per-visitor query fills it). */
public interface ClaimDiagnosisTotal {

    UUID getVisitorId();

    UUID getDiagnosisId();

    long getClaimCount();
}
