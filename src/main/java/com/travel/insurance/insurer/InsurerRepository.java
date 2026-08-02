package com.travel.insurance.insurer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InsurerRepository extends JpaRepository<Insurer, UUID> {

    boolean existsByName(String name);
}
