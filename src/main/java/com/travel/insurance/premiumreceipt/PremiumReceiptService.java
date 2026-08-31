package com.travel.insurance.premiumreceipt;

import com.travel.insurance.premiumreceipt.dto.PremiumReceiptPatchRequest;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;

public interface PremiumReceiptService {

    PremiumReceiptResponse get();

    PremiumReceiptResponse patch(PremiumReceiptPatchRequest request);
}
