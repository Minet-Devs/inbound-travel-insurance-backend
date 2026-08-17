package com.travel.insurance.common.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-off startup job that encrypts pre-existing plaintext in sensitive
 * columns (and backfills the passport-number blind index) for environments
 * that had visitor/claim/biometric data before field-level encryption was
 * introduced. Disabled by default; enable via
 * {@code app.encryption.backfill.enabled=true} for exactly one deploy, then
 * disable it again. Idempotent: columns whose current value already decrypts
 * successfully are left untouched, so it's safe to re-run if interrupted.
 *
 * Reads and writes go through {@link JdbcTemplate} directly (not the JPA
 * repositories/entities), since the entities' {@code @Convert} converters
 * assume ciphertext and would fail decrypting the very plaintext this job
 * exists to encrypt.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.encryption.backfill", name = "enabled", havingValue = "true")
public class EncryptionBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final FieldEncryptionService fieldEncryptionService;
    private final BlindIndexService blindIndexService;

    public EncryptionBackfillRunner(JdbcTemplate jdbcTemplate,
                                    FieldEncryptionService fieldEncryptionService,
                                    BlindIndexService blindIndexService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fieldEncryptionService = fieldEncryptionService;
        this.blindIndexService = blindIndexService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("Starting field-encryption backfill (app.encryption.backfill.enabled=true)");
        int visitors = backfillVisitors();
        int biometrics = backfillTable("biometric_verifications",
                List.of("subject_id_number", "embeded_token"));
        int claims = backfillTable("claims",
                List.of("description", "prescription", "decision_reason"));
        int preauthorizations = backfillTable("preauthorizations",
                List.of("service_description", "decision_reason"));
        log.warn("Field-encryption backfill complete: visitors={}, biometric_verifications={}, "
                        + "claims={}, preauthorizations={} rows updated",
                visitors, biometrics, claims, preauthorizations);
    }

    private static final List<String> VISITOR_ENCRYPTED_COLUMNS = List.of(
            "full_name", "passport_number", "date_of_birth", "nationality", "address",
            "email", "phone_number", "underlying_conditions", "next_of_kin_name", "next_of_kin_phone");

    private int backfillVisitors() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, passport_number, passport_number_hash, "
                        + String.join(", ", VISITOR_ENCRYPTED_COLUMNS) + " from visitors");

        int updated = 0;
        for (Map<String, Object> row : rows) {
            Map<String, Object> changes = new LinkedHashMap<>();
            for (String column : VISITOR_ENCRYPTED_COLUMNS) {
                String current = (String) row.get(column);
                if (current != null && !fieldEncryptionService.isEncrypted(current)) {
                    changes.put(column, fieldEncryptionService.encrypt(current));
                }
            }
            if (row.get("passport_number_hash") == null) {
                String passportPlaintext = resolvePlaintext((String) row.get("passport_number"));
                changes.put("passport_number_hash", blindIndexService.hmac(passportPlaintext));
            }
            if (!changes.isEmpty()) {
                updateRow("visitors", row.get("id"), changes);
                updated++;
            }
        }
        return updated;
    }

    private int backfillTable(String table, List<String> columns) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, " + String.join(", ", columns) + " from " + table);

        int updated = 0;
        for (Map<String, Object> row : rows) {
            Map<String, Object> changes = new LinkedHashMap<>();
            for (String column : columns) {
                String current = (String) row.get(column);
                if (current != null && !fieldEncryptionService.isEncrypted(current)) {
                    changes.put(column, fieldEncryptionService.encrypt(current));
                }
            }
            if (!changes.isEmpty()) {
                updateRow(table, row.get("id"), changes);
                updated++;
            }
        }
        return updated;
    }

    private String resolvePlaintext(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        return fieldEncryptionService.isEncrypted(storedValue)
                ? fieldEncryptionService.decrypt(storedValue)
                : storedValue;
    }

    private void updateRow(String table, Object id, Map<String, Object> changes) {
        String setClause = changes.keySet().stream()
                .map(column -> column + " = ?")
                .reduce((a, b) -> a + ", " + b)
                .orElseThrow();
        Object[] params = new Object[changes.size() + 1];
        int i = 0;
        for (Object value : changes.values()) {
            params[i++] = value;
        }
        params[i] = id;
        jdbcTemplate.update("update " + table + " set " + setClause + " where id = ?", params);
    }
}
