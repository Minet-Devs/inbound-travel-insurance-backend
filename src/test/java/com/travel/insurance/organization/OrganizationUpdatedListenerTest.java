package com.travel.insurance.organization;

import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationUpdatedListenerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private InsurerService insurerService;

    @Mock
    private ServiceProviderService serviceProviderService;

    private OrganizationUpdatedListener listener;

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
    void onOrganizationUpdatedUpdatesMatchingInsurer() {
        listener = new OrganizationUpdatedListener(organizationService, insurerService, serviceProviderService);
        Organization organization = organization(OrganizationType.INSURER);
        UUID insurerId = UUID.randomUUID();
        when(organizationService.getEntityById(organization.getId())).thenReturn(organization);
        when(insurerService.findIdByOrganizationId(organization.getId())).thenReturn(Optional.of(insurerId));

        listener.onOrganizationUpdated(new OrganizationUpdatedEvent(organization.getId(), null, null));

        ArgumentCaptor<InsurerRequest> captor = ArgumentCaptor.forClass(InsurerRequest.class);
        verify(insurerService).update(eq(insurerId), captor.capture());
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
        verify(serviceProviderService, never()).update(any(), any());
    }

    @Test
    void onOrganizationUpdatedUpdatesMatchingServiceProvider() {
        listener = new OrganizationUpdatedListener(organizationService, insurerService, serviceProviderService);
        Organization organization = organization(OrganizationType.SERVICE_PROVIDER);
        UUID providerId = UUID.randomUUID();
        when(organizationService.getEntityById(organization.getId())).thenReturn(organization);
        when(serviceProviderService.findIdByOrganizationId(organization.getId())).thenReturn(Optional.of(providerId));
        when(serviceProviderService.getById(providerId)).thenReturn(new ServiceProviderResponse(providerId, "Acme",
                "contact@acme.example", "+254700000000", "Nairobi", organization.getId(), null, null, Instant.now(),
                Instant.now()));
        BigDecimal longitude = new BigDecimal("36.821946");
        BigDecimal latitude = new BigDecimal("-1.292066");

        listener.onOrganizationUpdated(new OrganizationUpdatedEvent(organization.getId(), longitude, latitude));

        ArgumentCaptor<ServiceProviderRequest> captor = ArgumentCaptor.forClass(ServiceProviderRequest.class);
        verify(serviceProviderService).update(eq(providerId), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Acme");
        assertThat(captor.getValue().contactEmail()).isEqualTo("contact@acme.example");
        assertThat(captor.getValue().organizationId()).isEqualTo(organization.getId());
        assertThat(captor.getValue().longitude()).isEqualTo(longitude);
        assertThat(captor.getValue().latitude()).isEqualTo(latitude);
        verify(insurerService, never()).update(any(), any());
    }

    @Test
    void onOrganizationUpdatedPreservesExistingLocationWhenEventOmitsIt() {
        listener = new OrganizationUpdatedListener(organizationService, insurerService, serviceProviderService);
        Organization organization = organization(OrganizationType.SERVICE_PROVIDER);
        UUID providerId = UUID.randomUUID();
        BigDecimal existingLongitude = new BigDecimal("36.821946");
        BigDecimal existingLatitude = new BigDecimal("-1.292066");
        when(organizationService.getEntityById(organization.getId())).thenReturn(organization);
        when(serviceProviderService.findIdByOrganizationId(organization.getId())).thenReturn(Optional.of(providerId));
        when(serviceProviderService.getById(providerId)).thenReturn(new ServiceProviderResponse(providerId, "Acme",
                "contact@acme.example", "+254700000000", "Nairobi", organization.getId(), existingLongitude,
                existingLatitude, Instant.now(), Instant.now()));

        listener.onOrganizationUpdated(new OrganizationUpdatedEvent(organization.getId(), null, null));

        ArgumentCaptor<ServiceProviderRequest> captor = ArgumentCaptor.forClass(ServiceProviderRequest.class);
        verify(serviceProviderService).update(eq(providerId), captor.capture());
        assertThat(captor.getValue().longitude()).isEqualTo(existingLongitude);
        assertThat(captor.getValue().latitude()).isEqualTo(existingLatitude);
    }

    @Test
    void onOrganizationUpdatedSkipsInsurerUpdateWhenNoneLinkedYet() {
        listener = new OrganizationUpdatedListener(organizationService, insurerService, serviceProviderService);
        Organization organization = organization(OrganizationType.INSURER);
        when(organizationService.getEntityById(organization.getId())).thenReturn(organization);
        when(insurerService.findIdByOrganizationId(organization.getId())).thenReturn(Optional.empty());

        listener.onOrganizationUpdated(new OrganizationUpdatedEvent(organization.getId(), null, null));

        verify(insurerService, never()).update(any(), any());
    }

    @Test
    void onOrganizationUpdatedIgnoresAdminType() {
        listener = new OrganizationUpdatedListener(organizationService, insurerService, serviceProviderService);
        Organization organization = organization(OrganizationType.ADMIN);
        when(organizationService.getEntityById(organization.getId())).thenReturn(organization);

        listener.onOrganizationUpdated(new OrganizationUpdatedEvent(organization.getId(), null, null));

        verify(insurerService, never()).update(any(), any());
        verify(serviceProviderService, never()).update(any(), any());
    }
}
