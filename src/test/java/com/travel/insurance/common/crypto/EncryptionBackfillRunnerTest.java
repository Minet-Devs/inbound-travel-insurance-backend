package com.travel.insurance.common.crypto;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionBackfillRunnerTest {

    private JdbcTemplate jdbcTemplate;
    private FieldEncryptionService fieldEncryptionService;
    private BlindIndexService blindIndexService;
    private EncryptionBackfillRunner runner;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                create table visitors (
                    id uuid primary key, full_name varchar(255), passport_number varchar(255),
                    date_of_birth varchar(255), nationality varchar(255), address varchar(255),
                    email varchar(255), phone_number varchar(255), underlying_conditions varchar(255),
                    next_of_kin_name varchar(255), next_of_kin_phone varchar(255),
                    passport_number_hash varchar(64))
                """);
        jdbcTemplate.execute("""
                create table biometric_verifications (
                    id uuid primary key, subject_id_number varchar(255), embeded_token varchar(255))
                """);
        jdbcTemplate.execute("""
                create table claims (
                    id uuid primary key, description varchar(255), prescription varchar(255),
                    decision_reason varchar(255))
                """);
        jdbcTemplate.execute("""
                create table preauthorizations (
                    id uuid primary key, service_description varchar(255), decision_reason varchar(255))
                """);

        byte[] dataKeyBytes = new byte[32];
        byte[] hashKeyBytes = new byte[32];
        new SecureRandom().nextBytes(dataKeyBytes);
        new SecureRandom().nextBytes(hashKeyBytes);
        EncryptionKeyProvider keyProvider = new EncryptionKeyProvider() {
            @Override
            public javax.crypto.SecretKey getDataKey() {
                return new SecretKeySpec(dataKeyBytes, "AES");
            }

            @Override
            public javax.crypto.SecretKey getBlindIndexKey() {
                return new SecretKeySpec(hashKeyBytes, "HmacSHA256");
            }
        };
        fieldEncryptionService = new FieldEncryptionService(keyProvider);
        blindIndexService = new BlindIndexService(keyProvider);
        runner = new EncryptionBackfillRunner(jdbcTemplate, fieldEncryptionService, blindIndexService);
    }

    @Test
    void encryptsPlaintextVisitorRowAndComputesPassportHash() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into visitors (id, full_name, passport_number, email, passport_number_hash)
                values (?, ?, ?, ?, null)
                """, id, "Jane Traveler", "P1234567", "jane.traveler@example.com");

        runner.run(null);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select full_name, passport_number, email, passport_number_hash from visitors where id = ?", id);

        assertThat((String) row.get("full_name")).isNotEqualTo("Jane Traveler");
        assertThat((String) row.get("passport_number")).isNotEqualTo("P1234567");
        assertThat((String) row.get("email")).isNotEqualTo("jane.traveler@example.com");
        assertThat(fieldEncryptionService.decrypt((String) row.get("full_name"))).isEqualTo("Jane Traveler");
        assertThat(fieldEncryptionService.decrypt((String) row.get("passport_number"))).isEqualTo("P1234567");
        assertThat((String) row.get("passport_number_hash")).isEqualTo(blindIndexService.hmac("P1234567"));
    }

    @Test
    void isIdempotentAndSkipsAlreadyEncryptedRows() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into visitors (id, full_name, passport_number, passport_number_hash)
                values (?, ?, ?, ?)
                """, id, "Jane Traveler", "P1234567", blindIndexService.hmac("P1234567"));

        runner.run(null);
        Map<String, Object> afterFirstRun = jdbcTemplate.queryForMap(
                "select full_name, passport_number from visitors where id = ?", id);

        runner.run(null);
        Map<String, Object> afterSecondRun = jdbcTemplate.queryForMap(
                "select full_name, passport_number from visitors where id = ?", id);

        assertThat(afterSecondRun).isEqualTo(afterFirstRun);
        assertThat(fieldEncryptionService.decrypt((String) afterSecondRun.get("full_name")))
                .isEqualTo("Jane Traveler");
    }

    @Test
    void encryptsPlaintextClaimColumns() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into claims (id, description, prescription, decision_reason) values (?, ?, ?, ?)",
                id, "Fractured wrist", "Ibuprofen 400mg", "Approved per policy limit");

        runner.run(null);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select description, prescription, decision_reason from claims where id = ?", id);
        assertThat(fieldEncryptionService.decrypt((String) row.get("description"))).isEqualTo("Fractured wrist");
        assertThat(fieldEncryptionService.decrypt((String) row.get("prescription"))).isEqualTo("Ibuprofen 400mg");
        assertThat(fieldEncryptionService.decrypt((String) row.get("decision_reason")))
                .isEqualTo("Approved per policy limit");
    }
}
