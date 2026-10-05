package com.travel.insurance.touristattraction.dto;

import jakarta.validation.constraints.NotBlank;

public record TouristAttractionRequest(
        @NotBlank String name,
        @NotBlank String county
) {
}
