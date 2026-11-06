package com.travel.insurance.preauthorization;

import java.math.BigDecimal;
import java.util.UUID;

/** Approved pre-authorization count and approved amount for one insurer or service provider ({@code getGroupId()}). */
public interface PreauthorizationDashboardTotal {

    UUID getGroupId();

    long getPreauthorizationCount();

    BigDecimal getApprovedAmount();
}
