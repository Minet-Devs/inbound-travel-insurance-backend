package com.travel.insurance.visitor.dto;

import java.time.LocalDate;
import java.util.List;

/** One trend bucket: visitors registered per insurer between {@code periodStart} and {@code periodEnd} (inclusive, UTC). */
public record InsurerVisitorTrendBucket(
        LocalDate periodStart,
        LocalDate periodEnd,
        List<InsurerVisitorCount> insurers
) {
}
