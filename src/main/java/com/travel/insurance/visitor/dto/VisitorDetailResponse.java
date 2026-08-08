package com.travel.insurance.visitor.dto;

import com.travel.insurance.visitor.Gender;
import com.travel.insurance.visitor.MaritalStatus;
import com.travel.insurance.visitor.VisitorStatus;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VisitorDetailResponse(
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
        List<VisitorBenefitResponse> visitorBenefits,
        Instant createdDate,
        Instant updatedDate
) {

    public static VisitorDetailResponse of(VisitorResponse visitor,
                                           List<VisitorBenefitResponse> visitorBenefits) {
        return new VisitorDetailResponse(
                visitor.id(),
                visitor.policyId(),
                visitor.fullName(),
                visitor.passportNumber(),
                visitor.dateOfBirth(),
                visitor.gender(),
                visitor.nationality(),
                visitor.address(),
                visitor.email(),
                visitor.phoneNumber(),
                visitor.dateIn(),
                visitor.dateOut(),
                visitor.maritalStatus(),
                visitor.reasonForTravel(),
                visitor.facePhotoUrl(),
                visitor.underlyingConditions(),
                visitor.visitorStatus(),
                visitor.nextOfKinName(),
                visitor.nextOfKinPhone(),
                visitorBenefits,
                visitor.createdDate(),
                visitor.updatedDate());
    }
}
