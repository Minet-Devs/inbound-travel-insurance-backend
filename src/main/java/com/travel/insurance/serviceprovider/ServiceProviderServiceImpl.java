package com.travel.insurance.serviceprovider;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceProviderServiceImpl implements ServiceProviderService {

    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderMapper serviceProviderMapper;

    @Override
    public ServiceProviderResponse create(ServiceProviderRequest request) {
        if (serviceProviderRepository.existsByName(request.name())) {
            throw new IllegalStateException("Service provider already exists: " + request.name());
        }
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
        serviceProviderMapper.updateEntity(provider, request);
        return serviceProviderMapper.toResponse(serviceProviderRepository.save(provider));
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

    private ServiceProvider getEntity(UUID id) {
        return serviceProviderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", id));
    }
}
