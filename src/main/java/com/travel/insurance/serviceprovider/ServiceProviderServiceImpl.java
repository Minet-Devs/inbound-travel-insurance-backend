package com.travel.insurance.serviceprovider;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderNearbyResponse;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceProviderServiceImpl implements ServiceProviderService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper serviceProviderMapper;
    private final OrganizationService organizationService;

    @Override
    public ServiceProviderResponse create(ServiceProviderRequest request) {
        if (serviceProviderRepository.existsByName(request.name())) {
            throw new IllegalStateException("Service provider already exists: " + request.name());
        }
        validateOrganization(request.organizationId());
        ServiceProvider provider = serviceProviderMapper.toEntity(request);
        return serviceProviderMapper.toResponse(serviceProviderRepository.save(provider));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceProviderResponse getById(UUID id) {
        return serviceProviderMapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceProviderResponse> list(Pageable pageable) {
        return serviceProviderRepository.findAll(pageable).map(serviceProviderMapper::toResponse);
    }

    @Override
    public ServiceProviderResponse update(UUID id, ServiceProviderRequest request) {
        ServiceProvider provider = getEntity(id);
        validateOrganization(request.organizationId());
        serviceProviderMapper.updateEntity(provider, request);
        return serviceProviderMapper.toResponse(serviceProviderRepository.save(provider));
    }

    private void validateOrganization(UUID organizationId) {
        if (organizationId != null) {
            organizationService.getEntityById(organizationId);
        }
    }

    @Override
    public void delete(UUID id) {
        serviceProviderRepository.delete(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return serviceProviderRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> namesByIds(Collection<UUID> serviceProviderIds) {
        if (serviceProviderIds.isEmpty()) {
            return Map.of();
        }
        return serviceProviderRepository.findAllById(serviceProviderIds).stream()
                .collect(Collectors.toMap(ServiceProvider::getId, ServiceProvider::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceProviderResponse> searchByName(String name, int limit) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by("name").ascending());
        return serviceProviderRepository.findByNameContainingIgnoreCase(name.trim(), pageable)
                .map(serviceProviderMapper::toResponse)
                .getContent();
    }

    private ServiceProvider getEntity(UUID id) {
        return serviceProviderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findIdByOrganizationId(UUID organizationId) {
        return serviceProviderRepository.findFirstByOrganizationId(organizationId).map(ServiceProvider::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceProviderNearbyResponse> findNearby(BigDecimal latitude, BigDecimal longitude,
                                                            double radiusKm) {
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        return serviceProviderRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull().stream()
                .map(provider -> Map.entry(provider, haversineKm(lat, lng,
                        provider.getLatitude().doubleValue(), provider.getLongitude().doubleValue())))
                .filter(entry -> entry.getValue() <= radiusKm)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(entry -> serviceProviderMapper.toNearbyResponse(entry.getKey()))
                .toList();
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
