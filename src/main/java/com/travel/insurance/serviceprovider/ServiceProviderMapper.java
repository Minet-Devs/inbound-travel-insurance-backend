package com.travel.insurance.serviceprovider;

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
    }

    public ServiceProviderResponse toResponse(ServiceProvider provider) {
        return new ServiceProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getContactEmail(),
                provider.getContactPhone(),
                provider.getAddress(),
                provider.getOrganizationId(),
                provider.getCreatedDate(),
                provider.getUpdatedDate()
        );
    }
}
