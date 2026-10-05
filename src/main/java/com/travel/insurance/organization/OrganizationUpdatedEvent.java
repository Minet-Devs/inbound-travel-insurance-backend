package com.travel.insurance.organization;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * longitude/latitude are null when the update request didn't supply them —
 * OrganizationUpdatedListener treats that as "leave the linked
 * ServiceProvider's current location unchanged" rather than clearing it.
 */
public record OrganizationUpdatedEvent(UUID organizationId, BigDecimal longitude, BigDecimal latitude) {
}
