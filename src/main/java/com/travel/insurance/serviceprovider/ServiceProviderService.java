package com.travel.insurance.serviceprovider;

import com.travel.insurance.serviceprovider.dto.ServiceProviderNearbyResponse;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ServiceProviderService {

    ServiceProviderResponse create(ServiceProviderRequest request);

    ServiceProviderResponse getById(UUID id);

    Page<ServiceProviderResponse> list(Pageable pageable);

    ServiceProviderResponse update(UUID id, ServiceProviderRequest request);

    void delete(UUID id);

    boolean exists(UUID id);

    Map<UUID, String> namesByIds(Collection<UUID> serviceProviderIds);

    List<ServiceProviderResponse> searchByName(String name, int limit);

    Optional<UUID> findIdByOrganizationId(UUID organizationId);

    List<ServiceProviderNearbyResponse> findNearby(BigDecimal latitude, BigDecimal longitude, double radiusKm);
}
