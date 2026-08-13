package com.travel.insurance.preauthorization;

import com.travel.insurance.benefit.Benefit;
import com.travel.insurance.icd11.Icd11Code;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.preauthorization.dto.PreauthorizationItemRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationItemResponse;
import com.travel.insurance.preauthorization.dto.PreauthorizationRequest;
import com.travel.insurance.preauthorization.dto.PreauthorizationResponse;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.Visitor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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

    public PreauthorizationEnhancement toEnhancement(UUID preauthorizationId, PreauthorizationRequest request) {
        PreauthorizationEnhancement enhancement = new PreauthorizationEnhancement();
        enhancement.setPreauthorizationId(preauthorizationId);
        enhancement.setMedicalServiceId(request.medicalServiceId());
        enhancement.setRequestedAmount(request.requestedAmount());
        return enhancement;
    }

    public PreauthorizationItem toItem(PreauthorizationItemRequest request, UUID enhancementId) {
        PreauthorizationItem item = new PreauthorizationItem();
        item.setEnhancementId(enhancementId);
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setAmount(request.amount());
        item.setServiceDate(request.serviceDate());
        return item;
    }

    public PreauthorizationResponse toResponse(Preauthorization preauthorization, Policy policy, Visitor visitor,
                                               Icd11Code icd11Code, Benefit benefit,
                                               ServiceProviderResponse serviceProvider, UUID medicalServiceId,
                                               String medicalServiceName, List<PreauthorizationItem> items) {
        boolean decided = preauthorization.getStatus() != PreauthorizationStatus.PENDING;
        return new PreauthorizationResponse(
                preauthorization.getId(),
                preauthorization.getPolicyId(),
                policy.getPolicyNumber(),
                preauthorization.getVisitorId(),
                visitor != null ? visitor.getFullName() : null,
                preauthorization.getIcd11CodeId(),
                icd11Code != null ? icd11Code.getCode() : null,
                icd11Code != null ? icd11Code.getTitle() : null,
                preauthorization.getBenefitId(),
                benefit.getBenefitName(),
                preauthorization.getServiceProviderId(),
                serviceProvider.name(),
                medicalServiceId,
                medicalServiceName,
                preauthorization.getRequestedAmount(),
                preauthorization.getApprovedAmount(),
                preauthorization.getServiceDescription(),
                preauthorization.getDecisionReason(),
                preauthorization.getStatus(),
                preauthorization.getCreatedDate(),
                preauthorization.getUpdatedDate(),
                decided ? preauthorization.getUpdatedBy() : null,
                decided ? preauthorization.getUpdatedDate() : null,
                items.stream().map(this::toItemResponse).toList()
        );
    }

    private PreauthorizationItemResponse toItemResponse(PreauthorizationItem item) {
        return new PreauthorizationItemResponse(
                item.getId(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getAmount(),
                item.getServiceDate()
        );
    }
}