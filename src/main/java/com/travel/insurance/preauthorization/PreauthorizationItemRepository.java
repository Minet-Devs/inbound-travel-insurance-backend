package com.travel.insurance.preauthorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PreauthorizationItemRepository extends JpaRepository<PreauthorizationItem, UUID> {

    List<PreauthorizationItem> findAllByEnhancementId(UUID enhancementId);
}