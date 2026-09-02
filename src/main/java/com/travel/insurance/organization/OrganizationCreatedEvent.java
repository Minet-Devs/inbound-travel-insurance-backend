package com.travel.insurance.organization;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * longitude/latitude are carried here rather than on the Organization entity
 * itself — they are only ever persisted on the provisioned ServiceProvider,
 * see OrganizationCreatedListener.
 */
public record OrganizationCreatedEvent(UUID organizationId, BigDecimal longitude, BigDecimal latitude) {
}
