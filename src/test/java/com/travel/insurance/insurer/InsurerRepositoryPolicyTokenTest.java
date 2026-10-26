package com.travel.insurance.insurer;

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
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, EnvEncryptionKeyProvider.class, FieldEncryptionService.class,
        EncryptedStringConverter.class, EncryptedLocalDateConverter.class})
@ActiveProfiles("test")
class InsurerRepositoryPolicyTokenTest {

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID saveInsurer(String name, Long token) {
        Insurer insurer = new Insurer();
        insurer.setName(name);
        insurer.setContactEmail(name + "@example.com");
        insurer.setPolicyToken(token);
        return insurerRepository.saveAndFlush(insurer).getId();
    }

    @Test
    void consumeOnePolicyTokenDecrementsByOneAndReportsIt() {
        UUID id = saveInsurer("acme", 5L);

        int updated = insurerRepository.consumeOnePolicyToken(id);

        assertThat(updated).isEqualTo(1);
        assertThat(insurerRepository.findPolicyTokenById(id)).contains(4L);
    }

    @Test
    void consumeOnePolicyTokenNeverGoesBelowZero() {
        UUID id = saveInsurer("acme", 1L);

        assertThat(insurerRepository.consumeOnePolicyToken(id)).isEqualTo(1);
        assertThat(insurerRepository.consumeOnePolicyToken(id)).isEqualTo(0);

        assertThat(insurerRepository.findPolicyTokenById(id)).contains(0L);
    }

    @Test
    void consumeOnePolicyTokenIgnoresInsurerWithNullQuota() {
        UUID id = saveInsurer("acme", null);

        assertThat(insurerRepository.consumeOnePolicyToken(id)).isEqualTo(0);
    }

    @Test
    void consumeOnePolicyTokenOnlyTouchesTheGivenInsurer() {
        UUID target = saveInsurer("acme", 3L);
        UUID other = saveInsurer("globex", 3L);

        insurerRepository.consumeOnePolicyToken(target);

        assertThat(insurerRepository.findPolicyTokenById(other)).contains(3L);
    }

    @Test
    void findPolicyTokenByIdReadsTheDatabaseNotAStaleEntity() {
        UUID id = saveInsurer("acme", 5L);
        Insurer cached = insurerRepository.findById(id).orElseThrow();

        insurerRepository.consumeOnePolicyToken(id);

        assertThat(cached.getPolicyToken()).isEqualTo(5L);
        assertThat(insurerRepository.findPolicyTokenById(id)).contains(4L);
    }
}
