package com.travel.insurance.preauthorization;

import java.math.BigDecimal;
import java.util.UUID;

/** Pre-authorization count and requested/approved amounts for one visitor, rolled up to a country in the service. */
public interface PreauthorizationVisitorTotal {

    UUID getVisitorId();

    long getPreauthorizationCount();

    BigDecimal getRequestedAmount();

    BigDecimal getApprovedAmount();
}
