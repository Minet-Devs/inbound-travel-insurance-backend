package com.travel.insurance.visitor;

import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import org.springframework.stereotype.Component;

@Component
public class VisitorMapper {

    public Visitor toEntity(VisitorRequest request) {
        Visitor visitor = new Visitor();
        updateEntity(visitor, request);
        return visitor;
    }

    public void updateEntity(Visitor visitor, VisitorRequest request) {
        visitor.setPolicyId(request.policyId());
        visitor.setFullName(request.fullName());
        visitor.setPassportNumber(request.passportNumber());
        visitor.setDateOfBirth(request.dateOfBirth());
        visitor.setGender(request.gender());
        visitor.setNationality(request.nationality());
        visitor.setAddress(request.address());
        visitor.setEmail(request.email());
        visitor.setPhoneNumber(request.phoneNumber());
        visitor.setDateIn(request.dateIn());
        visitor.setDateOut(request.dateOut());
        visitor.setMaritalStatus(request.maritalStatus());
        visitor.setReasonForTravel(request.reasonForTravel());
        visitor.setFacePhotoUrl(request.facePhotoUrl());
        visitor.setUnderlyingConditions(request.underlyingConditions());
        visitor.setNextOfKinName(request.nextOfKinName());
        visitor.setNextOfKinPhone(request.nextOfKinPhone());
    }

    public VisitorResponse toResponse(Visitor visitor) {
        return new VisitorResponse(
                visitor.getId(),
                visitor.getPolicyId(),
                visitor.getFullName(),
                visitor.getPassportNumber(),
                visitor.getDateOfBirth(),
                visitor.getGender(),
                visitor.getNationality(),
                visitor.getAddress(),
                visitor.getEmail(),
                visitor.getPhoneNumber(),
                visitor.getDateIn(),
                visitor.getDateOut(),
                visitor.getMaritalStatus(),
                visitor.getReasonForTravel(),
                visitor.getFacePhotoUrl(),
                visitor.getUnderlyingConditions(),
                visitor.getVisitorStatus(),
                visitor.getNextOfKinName(),
                visitor.getNextOfKinPhone(),
                visitor.getCreatedDate(),
                visitor.getUpdatedDate()
        );
    }
}
