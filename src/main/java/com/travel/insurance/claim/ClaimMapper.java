package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.ClaimRequest;
import com.travel.insurance.claim.dto.ClaimResponse;
import org.springframework.stereotype.Component;

@Component
public class ClaimMapper {

    public Claim toEntity(ClaimRequest request) {
        Claim claim = new Claim();
        claim.setPolicyId(request.policyId());
        claim.setBenefitId(request.benefitId());
        claim.setServiceProviderId(request.serviceProviderId());
        claim.setPreauthorizationId(request.preauthorizationId());
        claim.setClaimedAmount(request.claimedAmount());
        claim.setDescription(request.description());
        return claim;
    }

    public ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getPolicyId(),
                claim.getBenefitId(),
                claim.getServiceProviderId(),
                claim.getPreauthorizationId(),
                claim.getClaimedAmount(),
                claim.getApprovedAmount(),
                claim.getDescription(),
                claim.getDecisionReason(),
                claim.getStatus(),
                claim.getCreatedDate(),
                claim.getUpdatedDate()
        );
    }
}
