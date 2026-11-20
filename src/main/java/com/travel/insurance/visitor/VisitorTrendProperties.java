package com.travel.insurance.visitor;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.visitor-trend")
public class VisitorTrendProperties {

    /** Day visitor data started streaming in; trend buckets before the one containing it are not returned. */
    private LocalDate dataStartDate = LocalDate.of(2026, 10, 5);
}
