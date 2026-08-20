package com.travel.insurance.organization;

import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerCreatedEvent;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.organization.dto.OrganizationRequest;
import com.travel.insurance.organization.dto.OrganizationResponse;
import com.travel.insurance.serviceprovider.ServiceProvider;
import com.travel.insurance.serviceprovider.ServiceProviderCreatedEvent;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the organization directory in sync with insurers and service providers:
 * on creation of either, provisions a matching {@link Organization} entry and
 * links it back via organizationId.
 */
@Component
@RequiredArgsConstructor
public class OrganizationProvisioningListener {

    private final InsurerService insurerService;
    private final ServiceProviderService serviceProviderService;
    private final OrganizationService organizationService;

    @EventListener
    @Transactional
    public void onInsurerCreated(InsurerCreatedEvent event) {
        Insurer insurer = insurerService.getEntityById(event.insurerId());
        OrganizationResponse organization = organizationService.create(new OrganizationRequest(
                insurer.getName(),
                OrganizationType.INSURER,
                insurer.getContactEmail(),
                insurer.getContactPhone(),
                insurer.getAddress(),
                null));
        insurerService.assignOrganizationId(insurer.getId(), organization.id());
    }

    @EventListener
    @Transactional
    public void onServiceProviderCreated(ServiceProviderCreatedEvent event) {
        ServiceProvider provider = serviceProviderService.getEntityById(event.serviceProviderId());
        OrganizationResponse organization = organizationService.create(new OrganizationRequest(
                provider.getName(),
                OrganizationType.SERVICE_PROVIDER,
                provider.getContactEmail(),
                provider.getContactPhone(),
                provider.getAddress(),
                null));
        serviceProviderService.assignOrganizationId(provider.getId(), organization.id());
    }
}
