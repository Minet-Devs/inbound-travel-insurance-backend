package com.travel.insurance.department;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.department.dto.DepartmentRequest;
import com.travel.insurance.department.dto.DepartmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new IllegalStateException("Department already exists: " + request.name());
        }
        Department department = departmentMapper.toEntity(request);
        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(UUID id) {
        return departmentMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> list(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(departmentMapper::toResponse);
    }

    @Override
    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        Department department = getEntityById(id);
        if (departmentRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalStateException("Department already exists: " + request.name());
        }
        departmentMapper.updateEntity(department, request);
        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    public void delete(UUID id) {
        departmentRepository.delete(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Department getEntityById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return departmentRepository.existsByName(name);
    }

    @Override
    public Department findOrCreateByName(String name) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setName(name);
                    return departmentRepository.save(department);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> namesByIds(Collection<UUID> departmentIds) {
        if (departmentIds.isEmpty()) {
            return Map.of();
        }
        return departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, UUID> idsByName(Collection<String> names) {
        Set<String> keys = names.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase())
                .collect(Collectors.toSet());
        if (keys.isEmpty()) {
            return Map.of();
        }
        return departmentRepository.findByLowerTrimmedNameIn(keys).stream()
                .collect(Collectors.toMap(department -> department.getName().trim().toLowerCase(),
                        Department::getId, (first, second) -> first));
    }
}