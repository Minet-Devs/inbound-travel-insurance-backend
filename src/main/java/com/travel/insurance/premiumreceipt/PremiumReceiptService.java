package com.travel.insurance.premiumreceipt;

import com.travel.insurance.premiumreceipt.dto.PremiumReceiptPatchRequest;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;

import java.math.BigDecimal;

public interface PremiumReceiptService {

    PremiumReceiptResponse get();

    PremiumReceiptResponse patch(PremiumReceiptPatchRequest request);

    /**
     * Resolves the premium rate for a visitor aged {@code ageInYears}: the
     * infant rate for 2 and below, the minor rate for 3 to 17, and the
     * standard rate for 18 and above.
     */
    BigDecimal calculateTotalPremium(int ageInYears);
}
