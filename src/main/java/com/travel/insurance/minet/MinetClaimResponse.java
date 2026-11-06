package com.travel.insurance.minet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minet's reply to an inbound claim. Only {@code success: true} means the claim was accepted. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MinetClaimResponse(Boolean success, String message, String claimId, String claimNumber) {
}
