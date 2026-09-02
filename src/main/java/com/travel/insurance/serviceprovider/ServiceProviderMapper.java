package com.travel.insurance.serviceprovider;

import com.travel.insurance.serviceprovider.dto.ServiceProviderNearbyResponse;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import org.springframework.stereotype.Component;

@Component
public class ServiceProviderMapper {

    public ServiceProvider toEntity(ServiceProviderRequest request) {
        ServiceProvider provider = new ServiceProvider();
        updateEntity(provider, request);
        return provider;
    }

    public void updateEntity(ServiceProvider provider, ServiceProviderRequest request) {
        provider.setName(request.name());
        provider.setContactEmail(request.contactEmail());
        provider.setContactPhone(request.contactPhone());
        provider.setAddress(request.address());
        provider.setOrganizationId(request.organizationId());
        provider.setLongitude(request.longitude());
        provider.setLatitude(request.latitude());
    }

    public ServiceProviderResponse toResponse(ServiceProvider provider) {
        return new ServiceProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getContactEmail(),
                provider.getContactPhone(),
                provider.getAddress(),
                provider.getOrganizationId(),
                provider.getLongitude(),
                provider.getLatitude(),
                provider.getCreatedDate(),
                provider.getUpdatedDate()
        );
    }

    public ServiceProviderNearbyResponse toNearbyResponse(ServiceProvider provider) {
        return new ServiceProviderNearbyResponse(
                provider.getId(),
                provider.getName(),
                provider.getLongitude(),
                provider.getLatitude(),
                provider.getContactEmail(),
                provider.getContactPhone()
        );
    }
}
