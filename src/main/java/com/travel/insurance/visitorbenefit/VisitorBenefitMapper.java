package com.travel.insurance.visitorbenefit;

import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.springframework.stereotype.Component;

@Component
public class VisitorBenefitMapper {

    public VisitorBenefitResponse toResponse(VisitorBenefit visitorBenefit) {
        return new VisitorBenefitResponse(
                visitorBenefit.getId(),
                visitorBenefit.getVisitorId(),
                visitorBenefit.getBenefitId(),
                visitorBenefit.getLimitAmount(),
                visitorBenefit.getCreatedDate(),
                visitorBenefit.getUpdatedDate()
        );
    }
}
