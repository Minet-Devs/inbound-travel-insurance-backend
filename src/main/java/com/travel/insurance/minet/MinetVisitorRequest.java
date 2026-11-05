package com.travel.insurance.minet;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.travel.insurance.visitor.Gender;

import java.time.LocalDate;
import java.util.Arrays;

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

    /**
     * Builds the Minet payload from visitor fields: the full name is split into first name (first token),
     * other names (middle tokens, null if none) and last name (last token); {@code dateJoined} is the entry
     * date, or today when it is not set. Missing name/gender yield blank/null fields instead of failing.
     */
    public static MinetVisitorRequest of(String passportNumber, String fullName, Gender gender,
                                         LocalDate dateOfBirth, LocalDate dateIn, String email,
                                         String phoneNumber, String nationality) {
        String[] names = (fullName == null ? "" : fullName.trim()).split("\\s+");
        String firstName = names[0];
        String lastName = names.length > 1 ? names[names.length - 1] : "";
        String otherNames = names.length > 2
                ? String.join(" ", Arrays.copyOfRange(names, 1, names.length - 1))
                : null;
        return new MinetVisitorRequest(passportNumber, firstName, lastName, otherNames,
                gender == null ? null : gender.name(), dateOfBirth, dateIn != null ? dateIn : LocalDate.now(),
                email, phoneNumber, nationality);
    }
}
