package com.travel.insurance.minet;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record MinetVisitorRequest(
        String memberNumber,
        String firstName,
        String lastName,
        String otherNames,
        String gender,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateOfBirth,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateJoined,
        String emailAddress,
        String primaryPhoneNumber,
        String residenceCountry) {
}
