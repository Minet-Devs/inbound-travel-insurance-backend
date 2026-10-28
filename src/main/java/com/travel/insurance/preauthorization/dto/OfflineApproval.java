package com.travel.insurance.preauthorization.dto;

import com.travel.insurance.common.domain.VisitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** An already-approved (offline) pre-authorization, as imported from the Excel upload. */
public record OfflineApproval(
        UUID visitorId,
        UUID benefitId,
        UUID serviceProviderId,
        BigDecimal amount,
        VisitType visitType,
        LocalDate treatmentDate,
        LocalDate dischargeDate
) {
}
