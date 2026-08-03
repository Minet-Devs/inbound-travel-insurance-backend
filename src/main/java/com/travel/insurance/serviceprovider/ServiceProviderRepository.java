package com.travel.insurance.serviceprovider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, UUID> {

    boolean existsByName(String name);
}
