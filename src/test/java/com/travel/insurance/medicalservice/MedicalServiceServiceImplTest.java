package com.travel.insurance.medicalservice;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.department.Department;
import com.travel.insurance.department.DepartmentService;
import com.travel.insurance.medicalservice.MedicalServiceExcelParser.MedicalServiceRow;
import com.travel.insurance.medicalservice.dto.MedicalServiceImportResult;
import com.travel.insurance.medicalservice.dto.MedicalServiceRequest;
import com.travel.insurance.medicalservice.dto.MedicalServiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalServiceServiceImplTest {

    @Mock
    private MedicalServiceRepository medicalServiceRepository;

    @Mock
    private MedicalServiceExcelParser medicalServiceExcelParser;

    @Mock
    private DepartmentService departmentService;

    private final MedicalServiceMapper medicalServiceMapper = new MedicalServiceMapper();

    private MedicalServiceServiceImpl medicalServiceService;

    private UUID departmentId;
    private Department department;

    @BeforeEach
    void setUp() {
        medicalServiceService = new MedicalServiceServiceImpl(
                medicalServiceRepository, medicalServiceMapper, medicalServiceExcelParser, departmentService);
        departmentId = UUID.randomUUID();
        department = new Department();
        department.setId(departmentId);
        department.setName("LABORATORY");
    }

    private MultipartFile anyXlsx() {
        return new MockMultipartFile("file", "services.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
    }

    @Test
    void createSavesAndReturnsServiceWithDepartmentName() {
        when(departmentService.getEntityById(departmentId)).thenReturn(department);
        when(medicalServiceRepository.existsByNameAndDepartmentId("LABORATORY GENERAL", departmentId))
                .thenReturn(false);
        when(medicalServiceRepository.save(any(MedicalService.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalServiceResponse response = medicalServiceService.create(
                new MedicalServiceRequest("LABORATORY GENERAL", departmentId));

        assertThat(response.name()).isEqualTo("LABORATORY GENERAL");
        assertThat(response.departmentId()).isEqualTo(departmentId);
        assertThat(response.departmentName()).isEqualTo("LABORATORY");
    }

    @Test
    void createRejectsDuplicateInSameDepartment() {
        when(departmentService.getEntityById(departmentId)).thenReturn(department);
        when(medicalServiceRepository.existsByNameAndDepartmentId("LABORATORY GENERAL", departmentId))
                .thenReturn(true);

        assertThatThrownBy(() -> medicalServiceService.create(
                new MedicalServiceRequest("LABORATORY GENERAL", departmentId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LABORATORY GENERAL");
        verify(medicalServiceRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(medicalServiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalServiceService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdResolvesDepartmentName() {
        UUID id = UUID.randomUUID();
        MedicalService entity = new MedicalService();
        entity.setId(id);
        entity.setName("LABORATORY GENERAL");
        entity.setDepartmentId(departmentId);
        when(medicalServiceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(departmentService.namesByIds(Set.of(departmentId))).thenReturn(Map.of(departmentId, "LABORATORY"));

        MedicalServiceResponse response = medicalServiceService.getById(id);

        assertThat(response.departmentName()).isEqualTo("LABORATORY");
    }

    @Test
    void listFiltersByDepartmentWhenProvided() {
        MedicalService entity = new MedicalService();
        entity.setDepartmentId(departmentId);
        entity.setName("LABORATORY GENERAL");
        Page<MedicalService> page = new PageImpl<>(List.of(entity));
        when(medicalServiceRepository.findAllByDepartmentId(departmentId, Pageable.unpaged())).thenReturn(page);
        when(departmentService.namesByIds(Set.of(departmentId))).thenReturn(Map.of(departmentId, "LABORATORY"));

        Page<MedicalServiceResponse> result = medicalServiceService.list(departmentId, Pageable.unpaged());

        assertThat(result).hasSize(1);
        verify(medicalServiceRepository).findAllByDepartmentId(departmentId, Pageable.unpaged());
        verify(medicalServiceRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listAllByDepartmentResolvesNames() {
        MedicalService entity = new MedicalService();
        entity.setDepartmentId(departmentId);
        entity.setName("LABORATORY GENERAL");
        when(medicalServiceRepository.findAllByDepartmentId(departmentId)).thenReturn(List.of(entity));
        when(departmentService.namesByIds(Set.of(departmentId))).thenReturn(Map.of(departmentId, "LABORATORY"));

        List<MedicalServiceResponse> result = medicalServiceService.listAllByDepartment(departmentId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().departmentName()).isEqualTo("LABORATORY");
    }

    @Test
    void updateAppliesChanges() {
        UUID id = UUID.randomUUID();
        MedicalService existing = new MedicalService();
        existing.setId(id);
        existing.setName("LABORATORY GENERAL");
        existing.setDepartmentId(departmentId);
        when(medicalServiceRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentService.getEntityById(departmentId)).thenReturn(department);
        when(medicalServiceRepository.existsByNameAndDepartmentIdAndIdNot("ALBUMIN LEVEL TEST", departmentId, id))
                .thenReturn(false);

        MedicalServiceResponse response = medicalServiceService.update(
                id, new MedicalServiceRequest("ALBUMIN LEVEL TEST", departmentId));

        assertThat(response.name()).isEqualTo("ALBUMIN LEVEL TEST");
        assertThat(response.departmentName()).isEqualTo("LABORATORY");
    }

    @Test
    void updateRejectsDuplicateInSameDepartment() {
        UUID id = UUID.randomUUID();
        MedicalService existing = new MedicalService();
        existing.setId(id);
        existing.setDepartmentId(departmentId);
        when(medicalServiceRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentService.getEntityById(departmentId)).thenReturn(department);
        when(medicalServiceRepository.existsByNameAndDepartmentIdAndIdNot("ALBUMIN LEVEL TEST", departmentId, id))
                .thenReturn(true);

        assertThatThrownBy(() -> medicalServiceService.update(
                id, new MedicalServiceRequest("ALBUMIN LEVEL TEST", departmentId)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteRemovesEntity() {
        UUID id = UUID.randomUUID();
        MedicalService existing = new MedicalService();
        existing.setId(id);
        when(medicalServiceRepository.findById(id)).thenReturn(Optional.of(existing));

        medicalServiceService.delete(id);

        verify(medicalServiceRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(medicalServiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalServiceService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(medicalServiceRepository, never()).delete(any());
    }

    @Test
    void importCreatesDepartmentsOnTheFlyAndInsertsServices() {
        when(medicalServiceExcelParser.parse(any())).thenReturn(List.of(
                new MedicalServiceRow("PHARMACY GENERAL", "PHARMACY"),
                new MedicalServiceRow("LABORATORY GENERAL", "LABORATORY"),
                new MedicalServiceRow("ALBUMIN LEVEL TEST", "LABORATORY")));

        Department pharmacy = new Department();
        pharmacy.setId(UUID.randomUUID());
        pharmacy.setName("PHARMACY");
        when(departmentService.existsByName("PHARMACY")).thenReturn(false);
        when(departmentService.findOrCreateByName("PHARMACY")).thenReturn(pharmacy);

        when(departmentService.existsByName("LABORATORY")).thenReturn(false);
        when(departmentService.findOrCreateByName("LABORATORY")).thenReturn(department);

        when(medicalServiceRepository.existsByNameAndDepartmentId(any(), any())).thenReturn(false);

        MedicalServiceImportResult result = medicalServiceService.importFromExcel(anyXlsx());

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.departmentsCreated()).isEqualTo(2);
        assertThat(result.servicesInserted()).isEqualTo(3);
        assertThat(result.servicesSkipped()).isZero();
        // LABORATORY is referenced by two rows but should only be resolved once.
        verify(departmentService, times(1)).findOrCreateByName("LABORATORY");
        verify(medicalServiceRepository, times(3)).save(any(MedicalService.class));
    }

    @Test
    void importSkipsRowsWithBlankServiceOrDepartment() {
        when(medicalServiceExcelParser.parse(any())).thenReturn(List.of(
                new MedicalServiceRow("PHARMACY GENERAL", "PHARMACY"),
                new MedicalServiceRow("", "PHARMACY"),
                new MedicalServiceRow("ORPHAN SERVICE", "")));

        Department pharmacy = new Department();
        pharmacy.setId(UUID.randomUUID());
        pharmacy.setName("PHARMACY");
        when(departmentService.existsByName("PHARMACY")).thenReturn(true);
        when(departmentService.findOrCreateByName("PHARMACY")).thenReturn(pharmacy);
        when(medicalServiceRepository.existsByNameAndDepartmentId(any(), any())).thenReturn(false);

        MedicalServiceImportResult result = medicalServiceService.importFromExcel(anyXlsx());

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.departmentsCreated()).isZero();
        assertThat(result.servicesInserted()).isEqualTo(1);
        assertThat(result.servicesSkipped()).isEqualTo(2);
    }

    @Test
    void importSkipsServiceAlreadyPresentInDepartment() {
        when(medicalServiceExcelParser.parse(any())).thenReturn(List.of(
                new MedicalServiceRow("LABORATORY GENERAL", "LABORATORY")));
        when(departmentService.existsByName("LABORATORY")).thenReturn(true);
        when(departmentService.findOrCreateByName("LABORATORY")).thenReturn(department);
        when(medicalServiceRepository.existsByNameAndDepartmentId("LABORATORY GENERAL", departmentId))
                .thenReturn(true);

        MedicalServiceImportResult result = medicalServiceService.importFromExcel(anyXlsx());

        assertThat(result.servicesInserted()).isZero();
        assertThat(result.servicesSkipped()).isEqualTo(1);
        verify(medicalServiceRepository, never()).save(any());
    }

    @Test
    void importRejectsEmptyFile() {
        MultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> medicalServiceService.importFromExcel(empty))
                .isInstanceOf(IllegalArgumentException.class);
        verify(medicalServiceExcelParser, never()).parse(any());
    }
}