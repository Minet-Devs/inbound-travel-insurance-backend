package com.travel.insurance.preauthorization;

import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import org.springframework.stereotype.Component;

@Component
public class PreauthorizationMapper {

    public Preauthorization toEntity(PreauthorizationRequest request) {
        Preauthorization preauthorization = new Preauthorization();
        preauthorization.setPolicyId(request.policyId());
        preauthorization.setBenefitId(request.benefitId());
        preauthorization.setServiceProviderId(request.serviceProviderId());
        preauthorization.setRequestedAmount(request.requestedAmount());
        preauthorization.setServiceDescription(request.serviceDescription());
        return preauthorization;
    }

    public PreauthorizationResponse toResponse(Preauthorization preauthorization) {
        return new PreauthorizationResponse(
                preauthorization.getId(),
                preauthorization.getPolicyId(),
                preauthorization.getBenefitId(),
                preauthorization.getServiceProviderId(),
                preauthorization.getRequestedAmount(),
                preauthorization.getApprovedAmount(),
                preauthorization.getServiceDescription(),
                preauthorization.getDecisionReason(),
                preauthorization.getStatus(),
                preauthorization.getCreatedDate(),
                preauthorization.getUpdatedDate()
        );
    }
}
