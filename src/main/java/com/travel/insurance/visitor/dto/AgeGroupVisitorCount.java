package com.travel.insurance.visitor.dto;

public record AgeGroupVisitorCount(
        String ageGroup,
        long totalVisitors
) {
}
