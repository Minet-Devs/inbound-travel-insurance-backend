package com.travel.insurance.visitor;

import com.travel.insurance.common.crypto.EncryptionKeyProvider;
import com.travel.insurance.common.crypto.FieldEncryptionService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorAgeBackfillRunnerTest {

    private JdbcTemplate jdbcTemplate;
    private FieldEncryptionService fieldEncryptionService;
    private VisitorAgeBackfillRunner runner;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                create table visitors (
                    id uuid primary key, date_of_birth varchar(255), date_in date, age integer)
                """);

        byte[] dataKeyBytes = new byte[32];
        byte[] hashKeyBytes = new byte[32];
        new SecureRandom().nextBytes(dataKeyBytes);
        new SecureRandom().nextBytes(hashKeyBytes);
        fieldEncryptionService = new FieldEncryptionService(new EncryptionKeyProvider() {
            @Override
            public javax.crypto.SecretKey getDataKey() {
                return new SecretKeySpec(dataKeyBytes, "AES");
            }

            @Override
            public javax.crypto.SecretKey getBlindIndexKey() {
                return new SecretKeySpec(hashKeyBytes, "HmacSHA256");
            }
        });
        runner = new VisitorAgeBackfillRunner(jdbcTemplate, fieldEncryptionService);
    }

    @Test
    void fillsAgeAtDateInFromEncryptedDateOfBirth() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into visitors (id, date_of_birth, date_in, age) values (?, ?, ?, null)",
                id, fieldEncryptionService.encrypt("2000-06-15"), java.sql.Date.valueOf("2026-06-14"));

        runner.run(null);

        assertThat(ageOf(id)).isEqualTo(25);
    }

    @Test
    void handlesPlaintextDateOfBirth() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into visitors (id, date_of_birth, date_in, age) values (?, ?, ?, null)",
                id, "2000-06-15", java.sql.Date.valueOf("2026-06-15"));

        runner.run(null);

        assertThat(ageOf(id)).isEqualTo(26);
    }

    @Test
    void leavesExistingAgeUntouched() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into visitors (id, date_of_birth, date_in, age) values (?, ?, ?, 99)",
                id, fieldEncryptionService.encrypt("2000-06-15"), java.sql.Date.valueOf("2026-06-14"));

        runner.run(null);

        assertThat(ageOf(id)).isEqualTo(99);
    }

    private Integer ageOf(UUID id) {
        return jdbcTemplate.queryForObject("select age from visitors where id = ?", Integer.class, id);
    }
}
