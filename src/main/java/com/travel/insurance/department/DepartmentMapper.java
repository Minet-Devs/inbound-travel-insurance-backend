package com.travel.insurance.department;

import com.travel.insurance.department.dto.DepartmentRequest;
import com.travel.insurance.department.dto.DepartmentResponse;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public Department toEntity(DepartmentRequest request) {
        Department department = new Department();
        updateEntity(department, request);
        return department;
    }

    public void updateEntity(Department department, DepartmentRequest request) {
        department.setName(request.name());
    }

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCreatedDate(),
                department.getUpdatedDate()
        );
    }
}