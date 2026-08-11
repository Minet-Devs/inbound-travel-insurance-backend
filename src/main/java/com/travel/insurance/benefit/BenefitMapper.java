package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitResponse;
import org.springframework.stereotype.Component;

@Component
public class BenefitMapper {

    public BenefitResponse toResponse(Benefit benefit) {
        return new BenefitResponse(
                benefit.getId(),
                benefit.getPolicyId(),
                benefit.getBenefitType(),
                benefit.getLimitAmount(),
                benefit.getCreatedDate(),
                benefit.getUpdatedDate()
        );
    }
}
