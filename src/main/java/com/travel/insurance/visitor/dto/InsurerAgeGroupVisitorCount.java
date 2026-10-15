package com.travel.insurance.visitor.dto;

import java.util.List;
import java.util.UUID;

public record InsurerAgeGroupVisitorCount(
        UUID insurerId,
        String insurerName,
        List<AgeGroupVisitorCount> ageGroups,
        long totalVisitors
) {
}
