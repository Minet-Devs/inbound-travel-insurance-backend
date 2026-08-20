package com.travel.insurance.insurer;

import com.travel.insurance.common.util.LogoUrlNormalizer;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InsurerMapper {

    public Insurer toEntity(InsurerRequest request) {
        Insurer insurer = new Insurer();
        updateEntity(insurer, request);
        return insurer;
    }

    public void updateEntity(Insurer insurer, InsurerRequest request) {
        insurer.setName(request.name());
        insurer.setContactEmail(request.contactEmail());
        insurer.setContactPhone(request.contactPhone());
        insurer.setAddress(request.address());
        insurer.setLogoUrl(LogoUrlNormalizer.normalize(request.logoUrl()));
        insurer.setPolicyToken(request.policyToken());
        insurer.setNotificationEmail(request.notificationEmail());
        insurer.setNotificationEmailPassword(request.notificationEmailPassword());
        insurer.setHost(request.host());
        insurer.setPort(request.port());
        insurer.setEsignature(request.esignature());
        insurer.setOrganizationId(request.organizationId());
    }

    public InsurerResponse toResponse(Insurer insurer, UUID policyId) {
        return new InsurerResponse(
                insurer.getId(),
                policyId,
                insurer.getName(),
                insurer.getContactEmail(),
                insurer.getContactPhone(),
                insurer.getAddress(),
                insurer.getLogoUrl(),
                insurer.getPolicyToken(),
                insurer.getPolicyToken() != null ? insurer.getPolicyToken() : 0L,
                insurer.getNotificationEmail(),
                insurer.getHost(),
                insurer.getPort(),
                insurer.getEsignature(),
                insurer.getOrganizationId(),
                insurer.getCreatedDate(),
                insurer.getUpdatedDate()
        );
    }
}
