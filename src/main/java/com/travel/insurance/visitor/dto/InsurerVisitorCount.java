package com.travel.insurance.visitor.dto;

import java.util.UUID;

public record InsurerVisitorCount(
        UUID insurerId,
        String insurerName,
        long totalVisitors
) {
}
