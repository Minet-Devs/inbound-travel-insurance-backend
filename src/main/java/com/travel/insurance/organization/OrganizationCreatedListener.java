package com.travel.insurance.organization;

import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions the matching Insurer/ServiceProvider whenever an Organization of
 * that type is created, linking it back via organizationId. For INSURER,
 * the logoUrl and notification email/e-signature settings are carried
 * across too. ADMIN-type organizations have no matching entity and are
 * ignored.
 */
@Component
@RequiredArgsConstructor
public class OrganizationCreatedListener {

    private final OrganizationService organizationService;
    private final InsurerService insurerService;
    private final ServiceProviderService serviceProviderService;

    @EventListener
    @Transactional
    public void onOrganizationCreated(OrganizationCreatedEvent event) {
        Organization organization = organizationService.getEntityById(event.organizationId());
        switch (organization.getOrganizationType()) {
            case INSURER -> insurerService.create(new InsurerRequest(
                    organization.getName(),
                    organization.getEmail(),
                    organization.getPhoneNumber(),
                    organization.getAddress(),
                    organization.getLogoUrl(),
                    null,
                    organization.getNotificationEmail(),
                    organization.getNotificationEmailPassword(),
                    organization.getHost(),
                    organization.getPort(),
                    organization.getEsignature(),
                    organization.getId()));
            case SERVICE_PROVIDER -> serviceProviderService.create(new ServiceProviderRequest(
                    organization.getName(),
                    organization.getEmail(),
                    organization.getPhoneNumber(),
                    organization.getAddress(),
                    organization.getId()));
            case ADMIN -> {
            }
        }
    }
}
