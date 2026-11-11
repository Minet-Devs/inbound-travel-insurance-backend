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
import static org.assertj.core.api.Assertions.tuple;

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

    @Test
    void countsClaimsPerDiagnosisAndPerVisitorAndDiagnosis() {
        UUID visitorA = UUID.randomUUID();
        UUID visitorB = UUID.randomUUID();
        UUID malaria = UUID.randomUUID();
        UUID cholera = UUID.randomUUID();
        Claim first = claim(visitorA, UUID.randomUUID(), "100.00", "1.00");
        first.setDiagnosisIds(Set.of(malaria, cholera));
        Claim second = claim(visitorB, UUID.randomUUID(), "100.00", "1.00");
        second.setDiagnosisIds(Set.of(malaria));
        claimRepository.save(first);
        claimRepository.save(second);
        claimRepository.save(claim(visitorA, UUID.randomUUID(), "100.00", "1.00"));
        claimRepository.flush();

        List<ClaimDiagnosisTotal> byDiagnosis = claimRepository.countByDiagnosis(null, null, null, null);
        List<ClaimDiagnosisTotal> byVisitor = claimRepository.countByVisitorAndDiagnosis(null, null, null, null);

        assertThat(byDiagnosis).extracting(ClaimDiagnosisTotal::getDiagnosisId, ClaimDiagnosisTotal::getClaimCount)
                .containsExactlyInAnyOrder(tuple(malaria, 2L),
                        tuple(cholera, 1L));
        assertThat(byVisitor).extracting(ClaimDiagnosisTotal::getVisitorId, ClaimDiagnosisTotal::getDiagnosisId,
                        ClaimDiagnosisTotal::getClaimCount)
                .containsExactlyInAnyOrder(tuple(visitorA, malaria, 1L),
                        tuple(visitorA, cholera, 1L),
                        tuple(visitorB, malaria, 1L));
    }
}
