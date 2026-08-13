package com.travel.insurance.procedure;

import com.travel.insurance.department.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Temporary fail-closed {@link DepartmentService} so the application boots on this
 * branch while the real Department feature is delivered by another team.
 *
 * <p>It is guarded by {@link ConditionalOnMissingBean}: as soon as the Department
 * team's implementation is on the classpath, Spring uses that and this fallback
 * backs off — no duplicate-bean conflict on merge. Until then this returns
 * {@code false} for every id, so procedure creation fails with a clear
 * "department is not valid" error rather than silently accepting unknown
 * departments. Remove this class once the real implementation lands.
 */
@Slf4j
@Configuration
public class DepartmentValidationFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(DepartmentService.class)
    public DepartmentService fallbackDepartmentService() {
        log.warn("No DepartmentService implementation found; using fail-closed fallback. "
                + "Procedure department validation will reject all departments until the "
                + "Department feature is wired in.");
        return id -> false;
    }
}
