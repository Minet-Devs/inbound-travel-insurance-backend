package com.travel.insurance.serviceprovider;

import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ServiceProviderService {

    ServiceProviderResponse create(ServiceProviderRequest request);

    ServiceProviderResponse getById(UUID id);

    Page<ServiceProviderResponse> list(Pageable pageable);

    ServiceProviderResponse update(UUID id, ServiceProviderRequest request);

    void delete(UUID id);

    boolean exists(UUID id);

    Map<UUID, String> namesByIds(Collection<UUID> serviceProviderIds);
}
