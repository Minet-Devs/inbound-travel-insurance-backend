package com.travel.insurance.visitor.dto;

import com.travel.insurance.visitor.VisitorStatus;
import jakarta.validation.constraints.NotNull;

public record VisitorStatusUpdate(
        @NotNull VisitorStatus visitorStatus
        ) {
}
