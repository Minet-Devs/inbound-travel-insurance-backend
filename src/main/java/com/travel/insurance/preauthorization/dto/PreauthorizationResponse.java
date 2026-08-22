package com.travel.insurance.preauthorization.dto;

import com.travel.insurance.preauthorization.PreauthorizationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PreauthorizationResponse(
        UUID id,
        UUID policyId,
        UUID visitorId,
        String visitorName,
        UUID icd11CodeId,
        String icd11Code,
        String icd11Title,
        UUID benefitId,
        String benefitName,
        UUID serviceProviderId,
        String serviceProviderName,
        UUID medicalServiceId,
        String medicalServiceName,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        String serviceDescription,
        String decisionReason,
        PreauthorizationStatus status,
        Instant createdDate,
        Instant updatedDate,
        UUID decidedBy,
        Instant decidedAt,
        List<PreauthorizationItemResponse> preauthorizationItems,
        Boolean convertedToClaim

) {
}
