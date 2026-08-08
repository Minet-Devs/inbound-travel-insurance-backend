package com.travel.insurance.visitor.dto;

import com.travel.insurance.visitor.Gender;
import com.travel.insurance.visitor.MaritalStatus;
import com.travel.insurance.visitor.VisitorStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VisitorResponse(
        UUID id,
        UUID policyId,
        String fullName,
        String passportNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String nationality,
        String address,
        String email,
        String phoneNumber,
        LocalDate dateIn,
        LocalDate dateOut,
        MaritalStatus maritalStatus,
        String reasonForTravel,
        String facePhotoUrl,
        String underlyingConditions,
        VisitorStatus visitorStatus,
        String nextOfKinName,
        String nextOfKinPhone,
        Instant createdDate,
        Instant updatedDate
) {
}
