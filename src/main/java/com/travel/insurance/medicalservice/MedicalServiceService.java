package com.travel.insurance.medicalservice;

import com.travel.insurance.medicalservice.dto.MedicalServiceImportResult;
import com.travel.insurance.medicalservice.dto.MedicalServiceRequest;
import com.travel.insurance.medicalservice.dto.MedicalServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MedicalServiceService {

    MedicalServiceResponse create(MedicalServiceRequest request);

    MedicalServiceResponse getById(UUID id);

    Page<MedicalServiceResponse> list(UUID departmentId, Pageable pageable);

    List<MedicalServiceResponse> listAllByDepartment(UUID departmentId);

    MedicalServiceResponse update(UUID id, MedicalServiceRequest request);

    void delete(UUID id);

    MedicalServiceImportResult importFromExcel(MultipartFile file);
}