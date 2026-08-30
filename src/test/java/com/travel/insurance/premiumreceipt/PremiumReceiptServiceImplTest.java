package com.travel.insurance.premiumreceipt;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptPatchRequest;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PremiumReceiptServiceImplTest {

    @Mock
    private PremiumReceiptRepository premiumReceiptRepository;

    private final PremiumReceiptMapper premiumReceiptMapper = new PremiumReceiptMapper();

    private PremiumReceiptServiceImpl premiumReceiptService;

    private PremiumReceipt existing;

    @BeforeEach
    void setUp() {
        premiumReceiptService = new PremiumReceiptServiceImpl(premiumReceiptRepository, premiumReceiptMapper);
        existing = new PremiumReceipt();
        existing.setTotalPremium(new BigDecimal("44"));
        existing.setPcfLevy(new BigDecimal("0.0001"));
        existing.setInsurancePremiumLevy(new BigDecimal("0.0005"));
        existing.setStampDuty(new BigDecimal("40"));
        existing.setTrainingLevy(new BigDecimal("0.001"));
    }

    @Test
    void getReturnsSingletonRecord() {
        when(premiumReceiptRepository.findById(PremiumReceiptServiceImpl.SINGLETON_ID))
                .thenReturn(Optional.of(existing));

        PremiumReceiptResponse response = premiumReceiptService.get();

        assertThat(response.totalPremium()).isEqualByComparingTo("44");
        assertThat(response.stampDuty()).isEqualByComparingTo("40");
    }

    @Test
    void getThrowsWhenSingletonRowMissing() {
        when(premiumReceiptRepository.findById(PremiumReceiptServiceImpl.SINGLETON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> premiumReceiptService.get())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void patchAppliesOnlyProvidedFields() {
        when(premiumReceiptRepository.findById(PremiumReceiptServiceImpl.SINGLETON_ID))
                .thenReturn(Optional.of(existing));
        when(premiumReceiptRepository.save(any(PremiumReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PremiumReceiptPatchRequest patchRequest =
                new PremiumReceiptPatchRequest(new BigDecimal("50"), null, null, null, null);

        PremiumReceiptResponse response = premiumReceiptService.patch(patchRequest);

        assertThat(response.totalPremium()).isEqualByComparingTo("50");
        assertThat(response.pcfLevy()).isEqualByComparingTo("0.0001");
        assertThat(response.insurancePremiumLevy()).isEqualByComparingTo("0.0005");
        assertThat(response.stampDuty()).isEqualByComparingTo("40");
        assertThat(response.trainingLevy()).isEqualByComparingTo("0.001");
    }

    @Test
    void patchThrowsWhenSingletonRowMissing() {
        when(premiumReceiptRepository.findById(PremiumReceiptServiceImpl.SINGLETON_ID))
                .thenReturn(Optional.empty());
        PremiumReceiptPatchRequest patchRequest =
                new PremiumReceiptPatchRequest(new BigDecimal("50"), null, null, null, null);

        assertThatThrownBy(() -> premiumReceiptService.patch(patchRequest))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(premiumReceiptRepository, never()).save(any());
    }
}
