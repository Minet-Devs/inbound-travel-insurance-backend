package com.travel.insurance.touristattraction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TouristAttractionRepository extends JpaRepository<TouristAttraction, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
