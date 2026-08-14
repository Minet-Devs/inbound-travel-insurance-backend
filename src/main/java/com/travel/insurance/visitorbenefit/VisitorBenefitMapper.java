package com.travel.insurance.visitorbenefit;

import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class VisitorBenefitMapper {

    public VisitorBenefitResponse toResponse(
            VisitorBenefit visitorBenefit, String benefitName, BigDecimal utilizedAmount) {
        BigDecimal limitAmount = visitorBenefit.getLimitAmount();
        return new VisitorBenefitResponse(
                visitorBenefit.getId(),
                visitorBenefit.getVisitorId(),
                visitorBenefit.getBenefitId(),
                benefitName,
                limitAmount,
                utilizedAmount,
                limitAmount.subtract(utilizedAmount),
                visitorBenefit.getStatus(),
                visitorBenefit.getCreatedDate(),
                visitorBenefit.getUpdatedDate()
        );
    }
}
