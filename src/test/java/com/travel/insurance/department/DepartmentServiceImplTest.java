package com.travel.insurance.department;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.department.dto.DepartmentRequest;
import com.travel.insurance.department.dto.DepartmentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private final DepartmentMapper departmentMapper = new DepartmentMapper();

    private DepartmentServiceImpl departmentService;

    private DepartmentRequest request;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentServiceImpl(departmentRepository, departmentMapper);
        request = new DepartmentRequest("LABORATORY");
    }

    @Test
    void createSavesAndReturnsDepartment() {
        when(departmentRepository.existsByName("LABORATORY")).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse response = departmentService.create(request);

        assertThat(response.name()).isEqualTo("LABORATORY");
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void createRejectsDuplicateName() {
        when(departmentRepository.existsByName("LABORATORY")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LABORATORY");
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        UUID id = UUID.randomUUID();
        Department existing = departmentMapper.toEntity(request);
        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByNameAndIdNot("PHARMACY", id)).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse response = departmentService.update(id, new DepartmentRequest("PHARMACY"));

        assertThat(response.name()).isEqualTo("PHARMACY");
    }

    @Test
    void updateRejectsNameAlreadyUsedByAnotherDepartment() {
        UUID id = UUID.randomUUID();
        Department existing = departmentMapper.toEntity(request);
        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByNameAndIdNot("PHARMACY", id)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.update(id, new DepartmentRequest("PHARMACY")))
                .isInstanceOf(IllegalStateException.class);
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void deleteRemovesEntity() {
        UUID id = UUID.randomUUID();
        Department existing = departmentMapper.toEntity(request);
        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));

        departmentService.delete(id);

        verify(departmentRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void existsByNameDelegatesToRepository() {
        when(departmentRepository.existsByName("LABORATORY")).thenReturn(true);

        assertThat(departmentService.existsByName("LABORATORY")).isTrue();
    }

    @Test
    void findOrCreateByNameReturnsExistingDepartment() {
        Department existing = departmentMapper.toEntity(request);
        when(departmentRepository.findByName("LABORATORY")).thenReturn(Optional.of(existing));

        Department result = departmentService.findOrCreateByName("LABORATORY");

        assertThat(result).isSameAs(existing);
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void findOrCreateByNameCreatesDepartmentWhenAbsent() {
        when(departmentRepository.findByName("PHARMACY")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Department result = departmentService.findOrCreateByName("PHARMACY");

        assertThat(result.getName()).isEqualTo("PHARMACY");
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void namesByIdsReturnsEmptyMapForEmptyInput() {
        assertThat(departmentService.namesByIds(Set.of())).isEmpty();
        verify(departmentRepository, never()).findAllById(any());
    }

    @Test
    void namesByIdsResolvesFromRepository() {
        Department department = departmentMapper.toEntity(request);
        UUID id = UUID.randomUUID();
        department.setId(id);
        when(departmentRepository.findAllById(Set.of(id))).thenReturn(List.of(department));

        Map<UUID, String> result = departmentService.namesByIds(Set.of(id));

        assertThat(result).containsEntry(id, "LABORATORY");
    }
}