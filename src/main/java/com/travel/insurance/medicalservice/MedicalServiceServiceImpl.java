package com.travel.insurance.medicalservice;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.department.Department;
import com.travel.insurance.department.DepartmentService;
import com.travel.insurance.medicalservice.MedicalServiceExcelParser.MedicalServiceRow;
import com.travel.insurance.medicalservice.dto.MedicalServiceImportResult;
import com.travel.insurance.medicalservice.dto.MedicalServiceRequest;
import com.travel.insurance.medicalservice.dto.MedicalServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalServiceServiceImpl implements MedicalServiceService {

    private final MedicalServiceRepository medicalServiceRepository;
    private final MedicalServiceMapper medicalServiceMapper;
    private final MedicalServiceExcelParser medicalServiceExcelParser;
    private final DepartmentService departmentService;

    @Override
    public MedicalServiceResponse create(MedicalServiceRequest request) {
        Department department = departmentService.getEntityById(request.departmentId());
        if (medicalServiceRepository.existsByNameAndDepartmentId(request.name(), request.departmentId())) {
            throw new IllegalStateException(
                    "Service already exists in this department: " + request.name());
        }
        MedicalService medicalService = medicalServiceMapper.toEntity(request);
        return medicalServiceMapper.toResponse(
                medicalServiceRepository.save(medicalService), department.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalServiceResponse getById(UUID id) {
        return toResponses(List.of(getEntityById(id))).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalServiceResponse> list(UUID departmentId, Pageable pageable) {
        Page<MedicalService> page = departmentId != null
                ? medicalServiceRepository.findAllByDepartmentId(departmentId, pageable)
                : medicalServiceRepository.findAll(pageable);
        return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalServiceResponse> listAllByDepartment(UUID departmentId) {
        return toResponses(medicalServiceRepository.findAllByDepartmentId(departmentId));
    }

    @Override
    public MedicalServiceResponse update(UUID id, MedicalServiceRequest request) {
        MedicalService medicalService = getEntityById(id);
        Department department = departmentService.getEntityById(request.departmentId());
        if (medicalServiceRepository.existsByNameAndDepartmentIdAndIdNot(
                request.name(), request.departmentId(), id)) {
            throw new IllegalStateException(
                    "Service already exists in this department: " + request.name());
        }
        medicalServiceMapper.updateEntity(medicalService, request);
        return medicalServiceMapper.toResponse(medicalService, department.getName());
    }

    @Override
    public void delete(UUID id) {
        medicalServiceRepository.delete(getEntityById(id));
    }

    @Override
    public MedicalServiceImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }

        List<MedicalServiceRow> rows;
        try {
            rows = medicalServiceExcelParser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded file", e);
        }

        Map<String, Department> departmentCache = new HashMap<>();
        int departmentsCreated = 0;
        int inserted = 0;
        int skipped = 0;
        for (MedicalServiceRow row : rows) {
            if (row.service().isBlank() || row.department().isBlank()) {
                skipped++;
                continue;
            }
            Department department = departmentCache.get(row.department());
            if (department == null) {
                boolean existed = departmentService.existsByName(row.department());
                department = departmentService.findOrCreateByName(row.department());
                if (!existed) {
                    departmentsCreated++;
                }
                departmentCache.put(row.department(), department);
            }
            if (medicalServiceRepository.existsByNameAndDepartmentId(row.service(), department.getId())) {
                skipped++;
                continue;
            }
            MedicalService medicalService = new MedicalService();
            medicalService.setName(row.service());
            medicalService.setDepartmentId(department.getId());
            medicalServiceRepository.save(medicalService);
            inserted++;
        }
        return new MedicalServiceImportResult(rows.size(), departmentsCreated, inserted, skipped);
    }

    private MedicalService getEntityById(UUID id) {
        return medicalServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalService", id));
    }

    private List<MedicalServiceResponse> toResponses(List<MedicalService> medicalServices) {
        Set<UUID> departmentIds = medicalServices.stream()
                .map(MedicalService::getDepartmentId)
                .collect(Collectors.toSet());
        Map<UUID, String> namesByIds = departmentService.namesByIds(departmentIds);
        return medicalServices.stream()
                .map(medicalService -> medicalServiceMapper.toResponse(
                        medicalService, namesByIds.get(medicalService.getDepartmentId())))
                .toList();
    }
}