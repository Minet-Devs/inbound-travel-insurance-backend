package com.travel.insurance.organization;

import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationCreatedListenerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private InsurerService insurerService;

    @Mock
    private ServiceProviderService serviceProviderService;

    private OrganizationCreatedListener listener;

    private Organization organization(OrganizationType type) {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName("Acme");
        organization.setOrganizationType(type);
        organization.setEmail("contact@acme.example");
        organization.setPhoneNumber("+254700000000");
        organization.setAddress("Nairobi");
        organization.setLogoUrl("https://cdn.example/acme.png");
        organization.setPolicyToken(123456L);
        organization.setNotificationEmail("notify@acme.example");
        organization.setNotificationEmailPassword("s3cr3t");
        organization.setHost("smtp.acme.example");
        organization.setPort(587);
        organization.setEsignature("signature-data");
        return organization;
    }

    @Test
    void onOrganizationCreatedCreatesInsurerForInsurerType() {
        listener = new OrganizationCreatedListener(organizationService, insurerService, serviceProviderService);
        Organization organization = organization(OrganizationType.INSURER);
        when(organizationService.getEntityById(organization.getId())).thenReturn(organization);

        listener.onOrganizationCreated(new OrganizationCreatedEvent(organization.getId()));

        ArgumentCaptor<InsurerRequest> captor = ArgumentCaptor.forClass(InsurerRequest.class);
        verify(insurerService).create(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Acme");
        assertThat(captor.getValue().contactEmail()).isEqualTo("contact@acme.example");
        assertThat(captor.getValue().contactPhone()).isEqualTo("+254700000000");
        assertThat(captor.getValue().address()).isEqualTo("Nairobi");
        assertThat(captor.getValue().logoUrl()).isEqualTo("https://cdn.example/acme.png");
        assertThat(captor.getValue().policyToken()).isEqualTo(123456L);
        assertThat(captor.getValue().notificationEmail()).isEqualTo("notify@acme.example");
        assertThat(captor.getValue().notificationEmailPassword()).isEqualTo("s3cr3t");
        assertThat(captor.getValue().host()).isEqualTo("smtp.acme.example");
        assertThat(captor.getValue().port()).isEqualTo(587);
        assertThat(captor.getValue().esignature()).isEqualTo("signature-data");
        assertThat(captor.getValue().organizationId()).isEqualTo(organization.getId());
        verify(serviceProviderService, never()).create(any());
    }

    @Test
    void onOrganizationCreatedCreatesServiceProviderForServiceProviderType() {
        listener = new OrganizationCreatedListener(organizationService, insurerService, serviceProviderService);
        Organization organization = organization(OrganizationType.SERVICE_PROVIDER);
        when(organizationService.getEntityById(organization.getId())).thenReturn(organization);

        listener.onOrganizationCreated(new OrganizationCreatedEvent(organization.getId()));

        ArgumentCaptor<ServiceProviderRequest> captor = ArgumentCaptor.forClass(ServiceProviderRequest.class);
        verify(serviceProviderService).create(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Acme");
        assertThat(captor.getValue().contactEmail()).isEqualTo("contact@acme.example");
        assertThat(captor.getValue().organizationId()).isEqualTo(organization.getId());
        verify(insurerService, never()).create(any());
    }

    @Test
    void onOrganizationCreatedIgnoresAdminType() {
        listener = new OrganizationCreatedListener(organizationService, insurerService, serviceProviderService);
        Organization organization = organization(OrganizationType.ADMIN);
        when(organizationService.getEntityById(organization.getId())).thenReturn(organization);

        listener.onOrganizationCreated(new OrganizationCreatedEvent(organization.getId()));

        verify(insurerService, never()).create(any());
        verify(serviceProviderService, never()).create(any());
    }
}
