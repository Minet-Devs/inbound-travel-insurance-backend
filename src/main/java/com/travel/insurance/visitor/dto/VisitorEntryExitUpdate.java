package com.travel.insurance.visitor.dto;

import java.time.Instant;

public record VisitorEntryExitUpdate(
        Instant entryTimestamp,
        Instant exitTimestamp
) {
}
