package com.travel.insurance.visitor.dto;

import com.travel.insurance.visitor.Gender;
import com.travel.insurance.visitor.MaritalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.UUID;

public record VisitorRequest(
        @NotNull UUID policyId,
        @NotBlank String fullName,
        @NotBlank String passportNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank String nationality,
        @NotBlank @Email String email,
        @NotBlank String phoneNumber,
        @NotNull LocalDate dateIn,
        @NotNull LocalDate dateOut,
        @NotNull MaritalStatus maritalStatus,
        String nextOfKinName,
        String nextOfKinPhone
) {
}
