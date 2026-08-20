package com.travel.insurance.organization;

import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerCreatedEvent;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.organization.dto.OrganizationRequest;
import com.travel.insurance.organization.dto.OrganizationResponse;
import com.travel.insurance.serviceprovider.ServiceProvider;
import com.travel.insurance.serviceprovider.ServiceProviderCreatedEvent;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationProvisioningListenerTest {

    @Mock
    private InsurerService insurerService;

    @Mock
    private ServiceProviderService serviceProviderService;

    @Mock
    private OrganizationService organizationService;

    private OrganizationProvisioningListener listener;

    @Test
    void onInsurerCreatedProvisionsOrganizationAndLinksBack() {
        listener = new OrganizationProvisioningListener(insurerService, serviceProviderService, organizationService);
        UUID insurerId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        Insurer insurer = new Insurer();
        insurer.setId(insurerId);
        insurer.setName("Acme Insurance");
        insurer.setContactEmail("contact@acme.example");
        insurer.setContactPhone("+254700000000");
        insurer.setAddress("Nairobi");
        when(insurerService.getEntityById(insurerId)).thenReturn(insurer);
        when(organizationService.create(any(OrganizationRequest.class)))
                .thenReturn(new OrganizationResponse(organizationId, "Acme Insurance", OrganizationType.INSURER,
                        "contact@acme.example", "+254700000000", "Nairobi", null, Instant.now(), Instant.now()));

        listener.onInsurerCreated(new InsurerCreatedEvent(insurerId));

        ArgumentCaptor<OrganizationRequest> captor = ArgumentCaptor.forClass(OrganizationRequest.class);
        verify(organizationService).create(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Acme Insurance");
        assertThat(captor.getValue().organizationType()).isEqualTo(OrganizationType.INSURER);
        assertThat(captor.getValue().email()).isEqualTo("contact@acme.example");
        verify(insurerService).assignOrganizationId(insurerId, organizationId);
    }

    @Test
    void onServiceProviderCreatedProvisionsOrganizationAndLinksBack() {
        listener = new OrganizationProvisioningListener(insurerService, serviceProviderService, organizationService);
        UUID providerId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        ServiceProvider provider = new ServiceProvider();
        provider.setId(providerId);
        provider.setName("Nairobi Hospital");
        provider.setContactEmail("contact@nairobihospital.example");
        provider.setContactPhone("+254711111111");
        provider.setAddress("Argwings Kodhek Rd");
        when(serviceProviderService.getEntityById(providerId)).thenReturn(provider);
        when(organizationService.create(any(OrganizationRequest.class)))
                .thenReturn(new OrganizationResponse(organizationId, "Nairobi Hospital",
                        OrganizationType.SERVICE_PROVIDER, "contact@nairobihospital.example", "+254711111111",
                        "Argwings Kodhek Rd", null, Instant.now(), Instant.now()));

        listener.onServiceProviderCreated(new ServiceProviderCreatedEvent(providerId));

        ArgumentCaptor<OrganizationRequest> captor = ArgumentCaptor.forClass(OrganizationRequest.class);
        verify(organizationService).create(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Nairobi Hospital");
        assertThat(captor.getValue().organizationType()).isEqualTo(OrganizationType.SERVICE_PROVIDER);
        verify(serviceProviderService).assignOrganizationId(providerId, organizationId);
    }
}
