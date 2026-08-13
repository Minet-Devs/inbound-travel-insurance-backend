package com.travel.insurance.medicalservice;

import com.travel.insurance.medicalservice.dto.MedicalServiceRequest;
import com.travel.insurance.medicalservice.dto.MedicalServiceResponse;
import org.springframework.stereotype.Component;

@Component
public class MedicalServiceMapper {

    public MedicalService toEntity(MedicalServiceRequest request) {
        MedicalService medicalService = new MedicalService();
        updateEntity(medicalService, request);
        return medicalService;
    }

    public void updateEntity(MedicalService medicalService, MedicalServiceRequest request) {
        medicalService.setName(request.name());
        medicalService.setDepartmentId(request.departmentId());
    }

    public MedicalServiceResponse toResponse(MedicalService medicalService, String departmentName) {
        return new MedicalServiceResponse(
                medicalService.getId(),
                medicalService.getName(),
                medicalService.getDepartmentId(),
                departmentName,
                medicalService.getCreatedDate(),
                medicalService.getUpdatedDate()
        );
    }
}