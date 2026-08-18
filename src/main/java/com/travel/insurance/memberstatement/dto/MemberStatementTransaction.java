package com.travel.insurance.memberstatement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MemberStatementTransaction(
        UUID claimId,
        LocalDate transactionDate,
        UUID benefitId,
        String benefitName,
        BigDecimal amount,
        String invoiceNumber,
        UUID serviceProviderId,
        String serviceProviderName
) {
}