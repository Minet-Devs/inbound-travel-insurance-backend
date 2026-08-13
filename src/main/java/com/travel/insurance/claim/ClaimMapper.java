package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.ClaimRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
@Component
public class ClaimMapper {

    public Claim toEntity(ClaimRequest request) {
        Claim claim = new Claim();
        claim.setPolicyId(request.policyId());
        claim.setBenefitId(request.benefitId());
        claim.setServiceProviderId(request.serviceProviderId());
        claim.setPreauthorizationId(request.preauthorizationId());
        claim.setVisitorId(request.visitorId());
        claim.setClaimedAmount(request.claimedAmount());
        claim.setDescription(request.description());
        claim.setPrescription(request.prescription());
        claim.setDiagnosisIds(orEmpty(request.diagnosisIds()));
        claim.setProcedureIds(orEmpty(request.procedureIds()));
        claim.setInvoiceIds(orEmpty(request.invoiceIds()));
        claim.setDocumentIds(orEmpty(request.documentIds()));
        return claim;
    }

    private static Set<UUID> orEmpty(Set<UUID> ids) {
        return ids == null ? new HashSet<>() : new HashSet<>(ids);
    }
}
