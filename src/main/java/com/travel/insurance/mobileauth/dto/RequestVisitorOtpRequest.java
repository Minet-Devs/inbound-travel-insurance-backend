package com.travel.insurance.mobileauth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestVisitorOtpRequest(
        @NotBlank @Email String email
) {
}
