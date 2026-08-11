package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import org.springframework.stereotype.Component;

@Component
public class BenefitMapper {

    public Benefit toEntity(BenefitRequest request) {
        Benefit benefit = new Benefit();
        updateEntity(benefit, request);
        return benefit;
    }

    public void updateEntity(Benefit benefit, BenefitRequest request) {
        benefit.setBenefitName(request.benefitName());
        benefit.setLimitAmount(request.limitAmount());
    }

    public BenefitResponse toResponse(Benefit benefit) {
        return new BenefitResponse(
                benefit.getId(),
                benefit.getBenefitName(),
                benefit.getLimitAmount(),
                benefit.getCreatedDate(),
                benefit.getUpdatedDate()
        );
    }
}
