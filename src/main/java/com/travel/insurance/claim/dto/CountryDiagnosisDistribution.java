package com.travel.insurance.claim.dto;

import java.util.List;

/**
 * Diagnoses of claims of visitors of one nationality (ISO alpha-3, upper-cased, {@code UNKNOWN} when none);
 * {@code totalDiagnoses} is the sum of the diagnosis counts, and percentages are relative to it.
 */
public record CountryDiagnosisDistribution(String country, long totalDiagnoses,
                                           List<DiagnosisDistribution> diagnoses) {
}
