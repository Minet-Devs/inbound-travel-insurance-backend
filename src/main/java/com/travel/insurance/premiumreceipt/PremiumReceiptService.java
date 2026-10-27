package com.travel.insurance.premiumreceipt;

import com.travel.insurance.premiumreceipt.dto.PremiumReceiptPatchRequest;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;

import java.math.BigDecimal;

public interface PremiumReceiptService {

    PremiumReceiptResponse get();

    PremiumReceiptResponse patch(PremiumReceiptPatchRequest request);

    /**
     * Returns the flat premium charged to every visitor; it does not vary by age.
     */
    BigDecimal calculateTotalPremium();
}
