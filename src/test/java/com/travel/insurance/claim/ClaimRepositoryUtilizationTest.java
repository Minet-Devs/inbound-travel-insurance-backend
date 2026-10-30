package com.travel.insurance.claim;

import com.travel.insurance.common.crypto.EncryptedLocalDateConverter;
import com.travel.insurance.common.crypto.EncryptedStringConverter;
import com.travel.insurance.common.crypto.EnvEncryptionKeyProvider;
import com.travel.insurance.common.crypto.FieldEncryptionService;
import com.travel.insurance.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, EnvEncryptionKeyProvider.class, FieldEncryptionService.class,
        EncryptedStringConverter.class, EncryptedLocalDateConverter.class})
@ActiveProfiles("test")
class ClaimRepositoryUtilizationTest {

    @Autowired
    private ClaimRepository claimRepository;

    private Claim claim(UUID visitorId, UUID benefitId, String claimed, String claimedBase) {
        Claim claim = new Claim();
        claim.setPolicyId(UUID.randomUUID());
        claim.setVisitorId(visitorId);
        claim.setBenefitId(benefitId);
        claim.setClaimedAmount(new BigDecimal(claimed));
        claim.setClaimedAmountBase(claimedBase == null ? null : new BigDecimal(claimedBase));
        return claim;
    }

    @Test
    void sumsClaimedAmountBaseNotLocalCurrencyAmount() {
        UUID visitorId = UUID.randomUUID();
        UUID benefitId = UUID.randomUUID();
        claimRepository.save(claim(visitorId, benefitId, "13000.00", "100.00"));
        claimRepository.save(claim(visitorId, benefitId, "6500.00", "50.00"));
        claimRepository.save(claim(visitorId, benefitId, "1300.00", null));
        claimRepository.flush();

        List<ClaimUtilizationTotal> totals =
                claimRepository.sumClaimedAmountsByVisitorAndBenefit(Set.of(visitorId), Set.of(benefitId));

        assertThat(totals).hasSize(1);
        assertThat(totals.getFirst().totalClaimed()).isEqualByComparingTo("150.00");
    }
}
