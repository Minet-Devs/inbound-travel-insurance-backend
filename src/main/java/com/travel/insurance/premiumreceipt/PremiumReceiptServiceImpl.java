package com.travel.insurance.premiumreceipt;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptPatchRequest;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PremiumReceiptServiceImpl implements PremiumReceiptService {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final PremiumReceiptRepository premiumReceiptRepository;
    private final PremiumReceiptMapper premiumReceiptMapper;

    @Override
    @Transactional(readOnly = true)
    public PremiumReceiptResponse get() {
        return premiumReceiptMapper.toResponse(getEntity());
    }

    @Override
    public PremiumReceiptResponse patch(PremiumReceiptPatchRequest request) {
        PremiumReceipt premiumReceipt = getEntity();
        premiumReceiptMapper.patchEntity(premiumReceipt, request);
        return premiumReceiptMapper.toResponse(premiumReceiptRepository.save(premiumReceipt));
    }

    private PremiumReceipt getEntity() {
        return premiumReceiptRepository.findById(SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("PremiumReceipt", SINGLETON_ID));
    }
}
