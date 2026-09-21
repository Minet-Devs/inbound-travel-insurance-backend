package com.travel.insurance.otp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyOtpRequest(
        @NotBlank @Email String email,
        @NotNull UUID serviceProviderId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code") String otp) {
}
