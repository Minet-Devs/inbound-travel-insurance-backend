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
 * Mirrors an Organization update (PUT or PATCH — both publish this same
 * event after saving) into its matching Insurer/ServiceProvider (found via
 * organizationId), the same way OrganizationCreatedListener provisions them
 * on create. If no matching entity exists yet — e.g. the organization was
 * created before this linkage, or is ADMIN-type — this is a no-op.
 */
@Component
@RequiredArgsConstructor
public class OrganizationUpdatedListener {

    private final OrganizationService organizationService;
    private final InsurerService insurerService;
    private final ServiceProviderService serviceProviderService;

    @EventListener
    @Transactional
    public void onOrganizationUpdated(OrganizationUpdatedEvent event) {
        Organization organization = organizationService.getEntityById(event.organizationId());
        switch (organization.getOrganizationType()) {
            case INSURER -> insurerService.findIdByOrganizationId(organization.getId())
                    .ifPresent(insurerId -> insurerService.update(insurerId, new InsurerRequest(
                            organization.getName(),
                            organization.getEmail(),
                            organization.getPhoneNumber(),
                            organization.getAddress(),
                            organization.getLogoUrl(),
                            organization.getPolicyToken(),
                            organization.getNotificationEmail(),
                            organization.getNotificationEmailPassword(),
                            organization.getHost(),
                            organization.getPort(),
                            organization.getEsignature(),
                            organization.getId())));
            case SERVICE_PROVIDER -> serviceProviderService.findIdByOrganizationId(organization.getId())
                    .ifPresent(providerId -> serviceProviderService.update(providerId, new ServiceProviderRequest(
                            organization.getName(),
                            organization.getEmail(),
                            organization.getPhoneNumber(),
                            organization.getAddress(),
                            organization.getId())));
            case ADMIN -> {
            }
        }
    }
}
