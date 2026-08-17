package com.travel.insurance.memberstatement.dto;

import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;

import java.util.List;
import java.util.UUID;

public record MemberStatementResponse(
        UUID visitorId,
        String memberName,
        String passportNumber,
        UUID policyId,
        String policyNumber,
        List<VisitorBenefitResponse> benefits,
        List<MemberStatementTransaction> transactions
) {
}