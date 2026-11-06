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
        provider.setCounty(request.county());
        provider.setOrganizationId(request.organizationId());
        provider.setLongitude(request.longitude());
        provider.setLatitude(request.latitude());
        // Null = not supplied (e.g. organization-driven updates): keep the stored value. Blank clears it.
        if (request.minetServiceProviderId() != null) {
            String minetId = request.minetServiceProviderId().trim();
            provider.setMinetServiceProviderId(minetId.isEmpty() ? null : minetId);
        }
    }

    public ServiceProviderResponse toResponse(ServiceProvider provider) {
        return new ServiceProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getContactEmail(),
                provider.getContactPhone(),
                provider.getAddress(),
                provider.getCounty(),
                provider.getOrganizationId(),
                provider.getLongitude(),
                provider.getLatitude(),
                provider.getCreatedDate(),
                provider.getUpdatedDate(),
                provider.getMinetServiceProviderId()
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
