package com.travel.insurance.visitor;

import com.travel.insurance.common.crypto.BlindIndexService;
import com.travel.insurance.common.crypto.EncryptedLocalDateConverter;
import com.travel.insurance.common.crypto.EncryptedStringConverter;
import com.travel.insurance.common.crypto.EnvEncryptionKeyProvider;
import com.travel.insurance.common.crypto.FieldEncryptionService;
import com.travel.insurance.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirms that Hibernate can resolve the Spring-managed AttributeConverter
 * beans (via SpringBeanContainer) and that a Visitor round-trips through
 * real encryption/decryption when persisted and reloaded, and that the
 * blind-index unique constraint enforces the same duplicate-passport rule
 * the old case-insensitive index did.
 */
@DataJpaTest
@Import({JpaAuditingConfig.class, EnvEncryptionKeyProvider.class, FieldEncryptionService.class,
        BlindIndexService.class, EncryptedStringConverter.class, EncryptedLocalDateConverter.class})
@ActiveProfiles("test")
class VisitorRepositoryEncryptionTest {

    @Autowired
    private VisitorRepository visitorRepository;

    @Autowired
    private BlindIndexService blindIndexService;

    @Autowired
    private EntityManager entityManager;

    private Visitor newVisitor(String passportNumber) {
        Visitor visitor = new Visitor();
        visitor.setPolicyId(UUID.randomUUID());
        visitor.setInsurerId(UUID.randomUUID());
        visitor.setFullName("Jane Traveler");
        visitor.setPassportNumber(passportNumber);
        visitor.setPassportNumberHash(blindIndexService.hmac(passportNumber));
        visitor.setDateOfBirth(LocalDate.of(1990, 5, 12));
        visitor.setGender(Gender.FEMALE);
        visitor.setNationality("Germany");
        visitor.setAddress("12 Example Street, Berlin");
        visitor.setEmail("jane.traveler@example.com");
        visitor.setPhoneNumber("+254700000000");
        visitor.setDateIn(LocalDate.of(2026, 8, 1));
        visitor.setDateOut(LocalDate.of(2026, 11, 1));
        visitor.setMaritalStatus(MaritalStatus.SINGLE);
        visitor.setReasonForTravel("Tourism");
        visitor.setFacePhotoUrl("https://storage.example.com/photos/jane.jpg");
        visitor.setUnderlyingConditions("Diabetes, requires insulin");
        visitor.setNextOfKinName("John Traveler");
        visitor.setNextOfKinPhone("+254711111111");
        return visitor;
    }

    @Test
    void encryptedFieldsRoundTripThroughPersistAndReload() {
        Visitor saved = visitorRepository.saveAndFlush(newVisitor("P1234567"));
        entityManager.clear();

        Object[] rawColumns = (Object[]) entityManager.createNativeQuery(
                        "select passport_number, email, date_of_birth from visitors where id = ?1")
                .setParameter(1, saved.getId())
                .getSingleResult();

        assertThat((String) rawColumns[0]).isNotEqualTo("P1234567");
        assertThat((String) rawColumns[1]).isNotEqualTo("jane.traveler@example.com");
        assertThat((String) rawColumns[2]).isNotEqualTo("1990-05-12");

        Visitor reloaded = visitorRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPassportNumber()).isEqualTo("P1234567");
        assertThat(reloaded.getEmail()).isEqualTo("jane.traveler@example.com");
        assertThat(reloaded.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 12));
        assertThat(reloaded.getUnderlyingConditions()).isEqualTo("Diabetes, requires insulin");
    }

    @Test
    void findByPassportNumberHashLocatesEncryptedVisitor() {
        visitorRepository.saveAndFlush(newVisitor("P7654321"));

        assertThat(visitorRepository.findByPassportNumberHash(blindIndexService.hmac("p7654321")))
                .isPresent()
                .get()
                .extracting(Visitor::getPassportNumber)
                .isEqualTo("P7654321");
    }

    @Test
    void duplicatePassportHashViolatesUniqueConstraint() {
        visitorRepository.saveAndFlush(newVisitor("P1112223"));

        assertThatThrownBy(() -> visitorRepository.saveAndFlush(newVisitor("p1112223")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
