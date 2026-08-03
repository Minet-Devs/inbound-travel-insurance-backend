package com.travel.insurance.visitorbenefit;

import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.springframework.stereotype.Component;

@Component
public class VisitorBenefitMapper {

    public VisitorBenefitResponse toResponse(VisitorBenefit visitorBenefit, String benefitName) {
        return new VisitorBenefitResponse(
                visitorBenefit.getId(),
                visitorBenefit.getVisitorId(),
                visitorBenefit.getBenefitId(),
                benefitName,
                visitorBenefit.getLimitAmount(),
                visitorBenefit.getCreatedDate(),
                visitorBenefit.getUpdatedDate()
        );
    }
}
