package com.travel.insurance.preauthorization;

/** Number of pre-authorizations in one status, for the dashboard summary. */
public interface PreauthorizationStatusCount {

    PreauthorizationStatus getStatus();

    long getStatusCount();
}
