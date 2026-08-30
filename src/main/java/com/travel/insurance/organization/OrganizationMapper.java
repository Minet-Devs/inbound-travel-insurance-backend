package com.travel.insurance.organization;

import com.travel.insurance.common.util.LogoUrlNormalizer;
import com.travel.insurance.organization.dto.OrganizationPatchRequest;
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
        organization.setLogoUrl(LogoUrlNormalizer.normalize(request.logoUrl()));
        organization.setPolicyToken(request.policyToken());
        organization.setNotificationEmail(request.notificationEmail());
        organization.setNotificationEmailPassword(request.notificationEmailPassword());
        organization.setHost(request.host());
        organization.setPort(request.port());
        organization.setEsignature(request.esignature());
    }

    public void patchEntity(Organization organization, OrganizationPatchRequest request) {
        if (request.name() != null) {
            organization.setName(request.name());
        }
        if (request.organizationType() != null) {
            organization.setOrganizationType(request.organizationType());
        }
        if (request.email() != null) {
            organization.setEmail(request.email());
        }
        if (request.phoneNumber() != null) {
            organization.setPhoneNumber(request.phoneNumber());
        }
        if (request.address() != null) {
            organization.setAddress(request.address());
        }
        if (request.city() != null) {
            organization.setCity(request.city());
        }
        if (request.logoUrl() != null) {
            organization.setLogoUrl(LogoUrlNormalizer.normalize(request.logoUrl()));
        }
        if (request.policyToken() != null) {
            organization.setPolicyToken(request.policyToken());
        }
        if (request.notificationEmail() != null) {
            organization.setNotificationEmail(request.notificationEmail());
        }
        if (request.notificationEmailPassword() != null) {
            organization.setNotificationEmailPassword(request.notificationEmailPassword());
        }
        if (request.host() != null) {
            organization.setHost(request.host());
        }
        if (request.port() != null) {
            organization.setPort(request.port());
        }
        if (request.esignature() != null) {
            organization.setEsignature(request.esignature());
        }
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
                organization.getLogoUrl(),
                organization.getPolicyToken(),
                organization.getNotificationEmail(),
                organization.getHost(),
                organization.getPort(),
                organization.getEsignature(),
                organization.getCreatedDate(),
                organization.getUpdatedDate()
        );
    }
}
