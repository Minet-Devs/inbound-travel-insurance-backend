package com.travel.insurance.serviceprovider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, UUID> {

    boolean existsByName(String name);

    List<ServiceProvider> findByNameContainingIgnoreCase(String name);

    Page<ServiceProvider> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

