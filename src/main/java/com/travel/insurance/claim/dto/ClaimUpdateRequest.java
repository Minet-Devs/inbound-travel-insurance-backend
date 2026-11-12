package com.travel.insurance.claim.dto;

import com.travel.insurance.common.domain.VisitType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * PUT /claims/{id}: the clinical details a provider or admin may correct while the claim is editable.
 * The claimed amount is not here — it always follows the claim's invoices.
 * {@code serviceProviderId}: null keeps the current provider; a different provider may only be set by
 * ADMIN or CONTACT_CENTER (a provider user would move the claim out of their own scope).
 */
public record ClaimUpdateRequest(
        @Size(max = 1000) String description,
        @Size(max = 2000) String prescription,
        @NotEmpty Set<UUID> diagnosisIds,
        Set<UUID> procedureIds,
        VisitType visitType,
        UUID serviceProviderId
) {
}
