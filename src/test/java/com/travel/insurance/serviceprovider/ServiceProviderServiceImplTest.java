package com.travel.insurance.serviceprovider;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.organization.Organization;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceProviderServiceImplTest {

    @Mock
    private ServiceProviderRepository serviceProviderRepository;

    @Mock
    private OrganizationService organizationService;

    private final ServiceProviderMapper serviceProviderMapper = new ServiceProviderMapper();

    private ServiceProviderServiceImpl serviceProviderService;

    private ServiceProviderRequest request;

    @BeforeEach
    void setUp() {
        serviceProviderService = new ServiceProviderServiceImpl(serviceProviderRepository, serviceProviderMapper,
                organizationService);
        request = new ServiceProviderRequest("Nairobi Hospital", "contact@nairobihospital.example",
                "+254700000000", "Argwings Kodhek Rd", null, new BigDecimal("36.821946"),
                new BigDecimal("-1.292066"));
    }

    @Test
    void createSavesAndReturnsServiceProvider() {
        when(serviceProviderRepository.existsByName("Nairobi Hospital")).thenReturn(false);
        when(serviceProviderRepository.save(any(ServiceProvider.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProviderResponse response = serviceProviderService.create(request);

        assertThat(response.name()).isEqualTo("Nairobi Hospital");
        assertThat(response.contactEmail()).isEqualTo("contact@nairobihospital.example");
        assertThat(response.contactPhone()).isEqualTo("+254700000000");
        assertThat(response.address()).isEqualTo("Argwings Kodhek Rd");
        verify(serviceProviderRepository).save(any(ServiceProvider.class));
    }

    @Test
    void createRejectsDuplicateName() {
        when(serviceProviderRepository.existsByName("Nairobi Hospital")).thenReturn(true);

        assertThatThrownBy(() -> serviceProviderService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nairobi Hospital");
        verify(serviceProviderRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        ServiceProviderRequest withOrganization = new ServiceProviderRequest("Nairobi Hospital",
                "contact@nairobihospital.example", null, null, organizationId, null, null);
        when(serviceProviderRepository.existsByName("Nairobi Hospital")).thenReturn(false);
        when(organizationService.getEntityById(organizationId))
                .thenThrow(new ResourceNotFoundException("Organization", organizationId));

        assertThatThrownBy(() -> serviceProviderService.create(withOrganization))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(serviceProviderRepository, never()).save(any());
    }

    @Test
    void createAcceptsExistingOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        ServiceProviderRequest withOrganization = new ServiceProviderRequest("Nairobi Hospital",
                "contact@nairobihospital.example", null, null, organizationId, null, null);
        when(serviceProviderRepository.existsByName("Nairobi Hospital")).thenReturn(false);
        when(organizationService.getEntityById(organizationId)).thenReturn(new Organization());
        when(serviceProviderRepository.save(any(ServiceProvider.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProviderResponse response = serviceProviderService.create(withOrganization);

        assertThat(response.organizationId()).isEqualTo(organizationId);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(serviceProviderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceProviderService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        UUID id = UUID.randomUUID();
        ServiceProvider existing = serviceProviderMapper.toEntity(request);
        when(serviceProviderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(serviceProviderRepository.save(any(ServiceProvider.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProviderRequest update = new ServiceProviderRequest("Aga Khan Hospital",
                "info@agakhan.example", null, null, null, null, null);
        ServiceProviderResponse response = serviceProviderService.update(id, update);

        assertThat(response.name()).isEqualTo("Aga Khan Hospital");
        assertThat(response.contactEmail()).isEqualTo("info@agakhan.example");
        assertThat(response.contactPhone()).isNull();
        assertThat(response.address()).isNull();
    }

    @Test
    void deleteRemovesEntity() {
        UUID id = UUID.randomUUID();
        ServiceProvider existing = serviceProviderMapper.toEntity(request);
        when(serviceProviderRepository.findById(id)).thenReturn(Optional.of(existing));

        serviceProviderService.delete(id);

        verify(serviceProviderRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(serviceProviderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceProviderService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(serviceProviderRepository, never()).delete(any());
    }

    @Test
    void existsDelegatesToRepository() {
        UUID id = UUID.randomUUID();
        when(serviceProviderRepository.existsById(id)).thenReturn(true);

        assertThat(serviceProviderService.exists(id)).isTrue();
    }

    @Test
    void namesByIdsResolvesNamesInBulk() {
        UUID id = UUID.randomUUID();
        ServiceProvider provider = serviceProviderMapper.toEntity(request);
        provider.setId(id);
        when(serviceProviderRepository.findAllById(Set.of(id))).thenReturn(List.of(provider));

        Map<UUID, String> names = serviceProviderService.namesByIds(Set.of(id));

        assertThat(names).containsEntry(id, "Nairobi Hospital");
    }

    @Test
    void namesByIdsReturnsEmptyMapForEmptyInput() {
        Map<UUID, String> names = serviceProviderService.namesByIds(Set.of());

        assertThat(names).isEmpty();
        verify(serviceProviderRepository, never()).findAllById(any());
    }

    @Test
    void searchByNameReturnsMatchingProviders() {
        ServiceProvider provider = serviceProviderMapper.toEntity(request);
        provider.setId(UUID.randomUUID());
        org.springframework.data.domain.Page<ServiceProvider> page =
                new org.springframework.data.domain.PageImpl<>(List.of(provider));
        when(serviceProviderRepository.findByNameContainingIgnoreCase(org.mockito.ArgumentMatchers.eq("Nairobi"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        List<ServiceProviderResponse> results = serviceProviderService.searchByName("Nairobi", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Nairobi Hospital");
    }

    @Test
    void searchByNameReturnsEmptyForBlankQuery() {
        List<ServiceProviderResponse> results = serviceProviderService.searchByName("   ", 5);

        assertThat(results).isEmpty();
        verify(serviceProviderRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void findNearbyReturnsProvidersWithinRadiusSortedByDistance() {
        ServiceProvider near = serviceProviderMapper.toEntity(request);
        near.setId(UUID.randomUUID());
        near.setName("Near Hospital");
        near.setLatitude(new BigDecimal("-1.30"));
        near.setLongitude(new BigDecimal("36.80"));

        ServiceProvider far = serviceProviderMapper.toEntity(request);
        far.setId(UUID.randomUUID());
        far.setName("Far Hospital");
        far.setLatitude(new BigDecimal("-1.90"));
        far.setLongitude(new BigDecimal("37.40"));

        when(serviceProviderRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of(far, near));

        List<com.travel.insurance.serviceprovider.dto.ServiceProviderNearbyResponse> results =
                serviceProviderService.findNearby(new BigDecimal("-1.30"), new BigDecimal("36.80"), 20.0);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Near Hospital");
    }

    @Test
    void findNearbyExcludesProvidersWithoutCoordinates() {
        when(serviceProviderRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull()).thenReturn(List.of());

        List<com.travel.insurance.serviceprovider.dto.ServiceProviderNearbyResponse> results =
                serviceProviderService.findNearby(new BigDecimal("-1.30"), new BigDecimal("36.80"), 20.0);

        assertThat(results).isEmpty();
    }
}
