package com.travel.insurance.visitor.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record VisitorEntryExitUpdate(
        @NotNull Instant entryTimestamp,
        @NotNull Instant exitTimestamp
) {
}
