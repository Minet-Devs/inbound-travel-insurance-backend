package com.travel.insurance.premiumreceipt;

import com.travel.insurance.premiumreceipt.dto.PremiumReceiptPatchRequest;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;
import org.springframework.stereotype.Component;

@Component
public class PremiumReceiptMapper {

    public void patchEntity(PremiumReceipt premiumReceipt, PremiumReceiptPatchRequest request) {
        if (request.totalPremium() != null) {
            premiumReceipt.setTotalPremium(request.totalPremium());
        }
        if (request.pcfLevy() != null) {
            premiumReceipt.setPcfLevy(request.pcfLevy());
        }
        if (request.insurancePremiumLevy() != null) {
            premiumReceipt.setInsurancePremiumLevy(request.insurancePremiumLevy());
        }
        if (request.stampDuty() != null) {
            premiumReceipt.setStampDuty(request.stampDuty());
        }
        if (request.trainingLevy() != null) {
            premiumReceipt.setTrainingLevy(request.trainingLevy());
        }
    }

    public PremiumReceiptResponse toResponse(PremiumReceipt premiumReceipt) {
        return new PremiumReceiptResponse(
                premiumReceipt.getId(),
                premiumReceipt.getTotalPremium(),
                premiumReceipt.getPcfLevy(),
                premiumReceipt.getInsurancePremiumLevy(),
                premiumReceipt.getStampDuty(),
                premiumReceipt.getTrainingLevy(),
                premiumReceipt.getCreatedDate(),
                premiumReceipt.getUpdatedDate()
        );
    }
}
