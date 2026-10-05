package com.travel.insurance.otp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendOtpRequest(
        @NotBlank @Email String email,
        @NotNull UUID serviceProviderId) {
}
