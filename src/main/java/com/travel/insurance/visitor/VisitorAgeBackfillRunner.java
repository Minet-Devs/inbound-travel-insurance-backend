package com.travel.insurance.visitor;

import com.travel.insurance.common.crypto.FieldEncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

/**
 * One-off startup job that fills {@code visitors.age} for rows created before the
 * column existed. {@code date_of_birth} is encrypted at rest, so this can't be a
 * SQL migration. Age is whole years as at {@code date_in}, matching
 * {@link Visitor#getAgeAtTravel()}. Disabled by default; enable via
 * {@code app.visitor-age.backfill.enabled=true} for one deploy, then disable it.
 * Idempotent: only rows with a null {@code age} are touched. Soft-deleted rows are
 * included, since it reads through {@link JdbcTemplate} rather than the entity.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.visitor-age.backfill", name = "enabled", havingValue = "true")
public class VisitorAgeBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final FieldEncryptionService fieldEncryptionService;

    public VisitorAgeBackfillRunner(JdbcTemplate jdbcTemplate, FieldEncryptionService fieldEncryptionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fieldEncryptionService = fieldEncryptionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("Starting visitor age backfill (app.visitor-age.backfill.enabled=true)");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, date_of_birth, date_in from visitors where age is null");

        int updated = 0;
        for (Map<String, Object> row : rows) {
            String dobStored = (String) row.get("date_of_birth");
            Object dateIn = row.get("date_in");
            if (dobStored == null || dateIn == null) {
                continue;
            }
            String dob = fieldEncryptionService.isEncrypted(dobStored)
                    ? fieldEncryptionService.decrypt(dobStored)
                    : dobStored;
            if (dob == null || dob.isEmpty()) {
                continue;
            }
            int age = Period.between(LocalDate.parse(dob), toLocalDate(dateIn)).getYears();
            jdbcTemplate.update("update visitors set age = ? where id = ?", age, row.get("id"));
            updated++;
        }
        log.warn("Visitor age backfill complete: {} rows updated", updated);
    }

    private static LocalDate toLocalDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : LocalDate.parse(value.toString());
    }
}
