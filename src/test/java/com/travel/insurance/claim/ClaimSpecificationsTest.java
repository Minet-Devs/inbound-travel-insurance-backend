package com.travel.insurance.claim;

import com.travel.insurance.common.crypto.BlindIndexService;
import com.travel.insurance.common.crypto.EncryptedLocalDateConverter;
import com.travel.insurance.common.crypto.EncryptedStringConverter;
import com.travel.insurance.common.crypto.EnvEncryptionKeyProvider;
import com.travel.insurance.common.crypto.FieldEncryptionService;
import com.travel.insurance.config.JpaAuditingConfig;
import com.travel.insurance.invoice.Invoice;
import com.travel.insurance.visitor.Gender;
import com.travel.insurance.visitor.MaritalStatus;
import com.travel.insurance.visitor.Visitor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs {@link ClaimSpecifications} and {@link ClaimRepositoryCustomImpl} against a real (H2) database,
 * since the subqueries and casts only prove themselves in SQL.
 */
@DataJpaTest
@Import({JpaAuditingConfig.class, EnvEncryptionKeyProvider.class, FieldEncryptionService.class,
        BlindIndexService.class, EncryptedStringConverter.class, EncryptedLocalDateConverter.class})
@ActiveProfiles("test")
class ClaimSpecificationsTest {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private BlindIndexService blindIndexService;

    @Autowired
    private EntityManager entityManager;

    private final UUID benefitId = UUID.randomUUID();
    private final UUID diagnosisId = UUID.randomUUID();
    private Visitor inStay;
    private Visitor outOfStay;

    /** Matches every flag: no pre-auth, submitted before the visitor arrived, invoice short of the claim. */
    private Claim flagged;
    /** Matches none: pre-authorized, inside the stay, invoice equals the claim. */
    private Claim clean;
    /** No invoices, no provider, sent to Minet. */
    private Claim bare;

    @BeforeEach
    void seed() {
        LocalDate today = LocalDate.now();
        inStay = persistVisitor("P-IN", today.minusDays(5), today.plusDays(5));
        outOfStay = persistVisitor("P-OUT", today.plusDays(5), today.plusDays(20));

        flagged = persistClaim(outOfStay.getId(), null, UUID.randomUUID(), "100.00", "10.00", ClaimStatus.SUBMITTED, false);
        flagged.getDiagnosisIds().add(diagnosisId);
        persistInvoice(flagged.getId(), "INV-FLAG-1", "60.00");

        clean = persistClaim(inStay.getId(), UUID.randomUUID(), UUID.randomUUID(), "50.00", "5.00", ClaimStatus.PAID, false);
        persistInvoice(clean.getId(), "INV-CLEAN-1", "20.00");
        persistInvoice(clean.getId(), "INV-CLEAN-2", "30.00");

        bare = persistClaim(inStay.getId(), UUID.randomUUID(), null, "75.00", "7.50", ClaimStatus.UNDER_REVIEW, true);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void eachAttentionFlagMatchesOnlyTheFlaggedClaim() {
        for (ClaimAttentionFlag flag : ClaimAttentionFlag.values()) {
            assertThat(ids(ClaimSpecifications.needsAttention(Set.of(flag))))
                    .as(flag.name())
                    .containsExactly(flagged.getId());
        }
    }

    @Test
    void invoiceMismatchIgnoresClaimsWithoutInvoices() {
        assertThat(ids(ClaimSpecifications.needsAttention(Set.of(ClaimAttentionFlag.INVOICE_MISMATCH))))
                .doesNotContain(bare.getId());
    }

    @Test
    void searchMatchesByClaimVisitorOrDiagnosis() {
        assertThat(ids(ClaimSpecifications.search(List.of(clean.getId()), List.of(), List.of())))
                .containsExactly(clean.getId());
        assertThat(ids(ClaimSpecifications.search(List.of(), List.of(inStay.getId()), List.of())))
                .containsExactlyInAnyOrder(clean.getId(), bare.getId());
        assertThat(ids(ClaimSpecifications.search(List.of(), List.of(), List.of(diagnosisId))))
                .containsExactly(flagged.getId());
        assertThat(ids(ClaimSpecifications.search(List.of(), List.of(), List.of()))).isEmpty();
    }

    @Test
    void providersIncludesUnlinkedOnlyWhenAsked() {
        UUID providerId = claimRepository.findById(clean.getId()).orElseThrow().getServiceProviderId();
        assertThat(ids(ClaimSpecifications.providers(List.of(providerId), false))).containsExactly(clean.getId());
        assertThat(ids(ClaimSpecifications.providers(List.of(providerId), true)))
                .containsExactlyInAnyOrder(clean.getId(), bare.getId());
    }

    @Test
    void amountSentToMinetAndDateFiltersApply() {
        assertThat(ids(ClaimSpecifications.amountBaseBetween(new BigDecimal("5.00"), new BigDecimal("7.50"))))
                .containsExactlyInAnyOrder(clean.getId(), bare.getId());
        assertThat(ids(ClaimSpecifications.sentToMinet(true))).containsExactly(bare.getId());
        Instant now = Instant.now();
        assertThat(ids(ClaimSpecifications.createdBetween(now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS))))
                .hasSize(3);
        assertThat(ids(ClaimSpecifications.createdBetween(now.plus(1, ChronoUnit.DAYS), null))).isEmpty();
    }

    @Test
    void aggregatesGroupAndSumOverTheSpecification() {
        Specification<Claim> notPaid = ClaimSpecifications.statusIn(
                EnumSet.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW));

        Map<ClaimStatus, Long> counts = claimRepository.countByStatus(Specification.allOf(List.of()));
        assertThat(counts).containsOnly(
                Map.entry(ClaimStatus.SUBMITTED, 1L), Map.entry(ClaimStatus.PAID, 1L),
                Map.entry(ClaimStatus.UNDER_REVIEW, 1L));
        assertThat(claimRepository.sumClaimedAmountBase(notPaid)).isEqualByComparingTo("17.50");
        assertThat(claimRepository.sumClaimedAmountBase(ClaimSpecifications.sentToMinet(true)
                .and(ClaimSpecifications.statusIn(Set.of(ClaimStatus.PAID))))).isEqualByComparingTo("0");
    }

    private List<UUID> ids(Specification<Claim> spec) {
        return claimRepository.findAll(spec).stream().map(Claim::getId).toList();
    }

    private Visitor persistVisitor(String passportNumber, LocalDate dateIn, LocalDate dateOut) {
        Visitor visitor = new Visitor();
        visitor.setPolicyId(UUID.randomUUID());
        visitor.setInsurerId(UUID.randomUUID());
        visitor.setFullName("Traveler " + passportNumber);
        visitor.setPassportNumber(passportNumber);
        visitor.setPassportNumberHash(blindIndexService.hmac(passportNumber));
        visitor.setDateOfBirth(LocalDate.of(1990, 5, 12));
        visitor.setGender(Gender.FEMALE);
        visitor.setNationality("Germany");
        visitor.setAddress("12 Example Street, Berlin");
        visitor.setEmail(passportNumber.toLowerCase() + "@example.com");
        visitor.setEmailHash(blindIndexService.hmac(passportNumber.toLowerCase() + "@example.com"));
        visitor.setPhoneNumber("+254700000000");
        visitor.setDateIn(dateIn);
        visitor.setDateOut(dateOut);
        visitor.setMaritalStatus(MaritalStatus.SINGLE);
        visitor.setReasonForTravel("Tourism");
        visitor.setFacePhotoUrl("https://storage.example.com/photos/" + passportNumber + ".jpg");
        visitor.setUnderlyingConditions("None");
        visitor.setNextOfKinName("Next Of Kin");
        visitor.setNextOfKinPhone("+254711111111");
        entityManager.persist(visitor);
        return visitor;
    }

    private Claim persistClaim(UUID visitorId, UUID preauthorizationId, UUID serviceProviderId, String amount,
                               String amountBase, ClaimStatus status, boolean sentToMinet) {
        Claim claim = new Claim();
        claim.setPolicyId(UUID.randomUUID());
        claim.setBenefitId(benefitId);
        claim.setVisitorId(visitorId);
        claim.setInsurerId(UUID.randomUUID());
        claim.setPreauthorizationId(preauthorizationId);
        claim.setServiceProviderId(serviceProviderId);
        claim.setClaimedAmount(new BigDecimal(amount));
        claim.setClaimedAmountBase(new BigDecimal(amountBase));
        claim.setStatus(status);
        claim.setSentToMinet(sentToMinet);
        entityManager.persist(claim);
        return claim;
    }

    private void persistInvoice(UUID claimId, String number, String total) {
        Invoice invoice = new Invoice();
        invoice.setClaimId(claimId);
        invoice.setInvoiceNumber(number);
        invoice.setCurrency("KES");
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setExchangeRate(new BigDecimal("0.0077"));
        invoice.setBaseTotalAmount(new BigDecimal(total));
        invoice.setFxRateDate(LocalDateTime.now());
        entityManager.persist(invoice);
    }
}
