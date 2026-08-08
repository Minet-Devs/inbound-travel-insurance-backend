package com.travel.insurance.visitorbenefit;

import com.travel.insurance.benefit.BenefitType;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.springframework.stereotype.Component;

@Component
public class VisitorBenefitMapper {

    public VisitorBenefitResponse toResponse(VisitorBenefit visitorBenefit, BenefitType benefitType) {
        return new VisitorBenefitResponse(
                visitorBenefit.getId(),
                visitorBenefit.getVisitorId(),
                visitorBenefit.getBenefitId(),
                benefitType,
                visitorBenefit.getLimitAmount(),
                visitorBenefit.getStatus(),
                visitorBenefit.getCreatedDate(),
                visitorBenefit.getUpdatedDate()
        );
    }
}
