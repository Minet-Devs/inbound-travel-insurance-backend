package com.travel.insurance.organization;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * longitude/latitude mirror the values submitted on the create request, so
 * they can be propagated to the provisioned ServiceProvider without
 * re-reading the saved Organization — see OrganizationCreatedListener.
 */
public record OrganizationCreatedEvent(UUID organizationId, BigDecimal longitude, BigDecimal latitude) {
}
