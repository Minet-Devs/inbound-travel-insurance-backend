package com.travel.insurance.organization;

import com.travel.insurance.organization.dto.OrganizationRequest;
import com.travel.insurance.organization.dto.OrganizationResponse;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public Organization toEntity(OrganizationRequest request) {
        Organization organization = new Organization();
        updateEntity(organization, request);
        return organization;
    }

    public void updateEntity(Organization organization, OrganizationRequest request) {
        organization.setName(request.name());
        organization.setOrganizationType(request.organizationType());
        organization.setEmail(request.email());
        organization.setPhoneNumber(request.phoneNumber());
        organization.setAddress(request.address());
        organization.setCity(request.city());
    }

    public OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getOrganizationType(),
                organization.getEmail(),
                organization.getPhoneNumber(),
                organization.getAddress(),
                organization.getCity(),
                organization.getCreatedDate(),
                organization.getUpdatedDate()
        );
    }
}
