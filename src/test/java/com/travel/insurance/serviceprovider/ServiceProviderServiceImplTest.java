package com.travel.insurance.serviceprovider;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
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

    private final ServiceProviderMapper serviceProviderMapper = new ServiceProviderMapper();

    private ServiceProviderServiceImpl serviceProviderService;

    private ServiceProviderRequest request;

    @BeforeEach
    void setUp() {
        serviceProviderService = new ServiceProviderServiceImpl(serviceProviderRepository, serviceProviderMapper);
        request = new ServiceProviderRequest("Nairobi Hospital", "contact@nairobihospital.example",
                "+254700000000", "Argwings Kodhek Rd");
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
                "info@agakhan.example", null, null);
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
}
