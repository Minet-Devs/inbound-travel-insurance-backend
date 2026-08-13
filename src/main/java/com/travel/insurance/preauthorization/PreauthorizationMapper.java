package com.travel.insurance.preauthorization;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.icd11.Icd11Code;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.Visitor;
import org.springframework.stereotype.Component;

@Component
public class PreauthorizationMapper {

    public Preauthorization toEntity(PreauthorizationRequest request) {
        Preauthorization preauthorization = new Preauthorization();
        preauthorization.setPolicyId(request.policyId());
        preauthorization.setVisitorId(request.visitorId());
        preauthorization.setIcd11CodeId(request.icd11CodeId());
        preauthorization.setBenefitId(request.benefitId());
        preauthorization.setServiceProviderId(request.serviceProviderId());
        preauthorization.setRequestedAmount(request.requestedAmount());
        preauthorization.setServiceDescription(request.serviceDescription());
        return preauthorization;
    }

    public PreauthorizationResponse toResponse(Preauthorization preauthorization, Policy policy, Visitor visitor,
                                               Icd11Code icd11Code, Benefit benefit,
                                               ServiceProviderResponse serviceProvider) {
        boolean decided = preauthorization.getStatus() != PreauthorizationStatus.PENDING;
        return new PreauthorizationResponse(
                preauthorization.getId(),
                preauthorization.getPolicyId(),
                policy.getPolicyNumber(),
                preauthorization.getVisitorId(),
                visitor.getFullName(),
                preauthorization.getIcd11CodeId(),
                icd11Code.getCode(),
                icd11Code.getTitle(),
                preauthorization.getBenefitId(),
                benefit.getBenefitName(),
                preauthorization.getServiceProviderId(),
                serviceProvider.name(),
                preauthorization.getRequestedAmount(),
                preauthorization.getApprovedAmount(),
                preauthorization.getServiceDescription(),
                preauthorization.getDecisionReason(),
                preauthorization.getStatus(),
                preauthorization.getCreatedDate(),
                preauthorization.getUpdatedDate(),
                decided ? preauthorization.getUpdatedBy() : null,
                decided ? preauthorization.getUpdatedDate() : null
        );
    }
}