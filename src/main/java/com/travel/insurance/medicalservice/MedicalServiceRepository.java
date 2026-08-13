package com.travel.insurance.medicalservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, UUID> {

    Page<MedicalService> findAllByDepartmentId(UUID departmentId, Pageable pageable);

    List<MedicalService> findAllByDepartmentId(UUID departmentId);

    boolean existsByNameAndDepartmentId(String name, UUID departmentId);

    boolean existsByNameAndDepartmentIdAndIdNot(String name, UUID departmentId, UUID id);
}