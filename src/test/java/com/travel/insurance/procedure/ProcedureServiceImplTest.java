package com.travel.insurance.procedure;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.department.Department;
import com.travel.insurance.department.DepartmentService;
import com.travel.insurance.procedure.dto.ProcedureRequest;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcedureServiceImplTest {

    @Mock
    private ProcedureRepository procedureRepository;
    @Mock
    private ProcedureCodeGenerator codeGenerator;
    @Mock
    private DepartmentService departmentService;

    private final ProcedureMapper procedureMapper = new ProcedureMapper();
    private final ProcedureNameNormalizer nameNormalizer = new ProcedureNameNormalizer();

    private ProcedureServiceImpl procedureService;

    private UUID departmentId;
    private ProcedureRequest request;

    @BeforeEach
    void setUp() {
        procedureService = new ProcedureServiceImpl(
                procedureRepository, procedureMapper, nameNormalizer, codeGenerator, departmentService);
        departmentId = UUID.randomUUID();
        request = new ProcedureRequest("  CHEST   TUBE INSERTION  ", "Insert a chest tube", departmentId);
    }

    private Procedure procedure(String display, String normalized, boolean active) {
        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID());
        procedure.setProcedureCode("PRC-0001");
        procedure.setName(display);
        procedure.setNormalizedName(normalized);
        procedure.setDepartmentPublicId(departmentId);
        procedure.setActive(active);
        return procedure;
    }

    @Test
    void createGeneratesCodeAndSavesActiveProcedure() {
        when(departmentService.getEntityById(departmentId)).thenReturn(new Department());
        when(procedureRepository.findByDepartmentPublicIdAndNormalizedName(departmentId, "CHEST TUBE INSERTION"))
                .thenReturn(Optional.empty());
        when(codeGenerator.next()).thenReturn("PRC-0001");
        when(procedureRepository.saveAndFlush(any(Procedure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcedureResponse response = procedureService.create(request);

        assertThat(response.procedureCode()).isEqualTo("PRC-0001");
        assertThat(response.name()).isEqualTo("CHEST TUBE INSERTION");
        assertThat(response.departmentPublicId()).isEqualTo(departmentId);
        assertThat(response.active()).isTrue();
        assertThat(response.uploadBatchPublicId()).isNull();
    }

    @Test
    void createRejectsInvalidDepartment() {
        when(departmentService.getEntityById(departmentId))
                .thenThrow(new ResourceNotFoundException("Department", departmentId));

        assertThatThrownBy(() -> procedureService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(procedureRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsBlankNameAfterCleaning() {
        when(departmentService.getEntityById(departmentId)).thenReturn(new Department());

        ProcedureRequest blank = new ProcedureRequest("   ", null, departmentId);
        assertThatThrownBy(() -> procedureService.create(blank))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void createRejectsActiveDuplicate() {
        when(departmentService.getEntityById(departmentId)).thenReturn(new Department());
        when(procedureRepository.findByDepartmentPublicIdAndNormalizedName(departmentId, "CHEST TUBE INSERTION"))
                .thenReturn(Optional.of(procedure("Chest Tube Insertion", "CHEST TUBE INSERTION", true)));

        assertThatThrownBy(() -> procedureService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        verify(procedureRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsInactiveDuplicateAdvisingReactivation() {
        when(departmentService.getEntityById(departmentId)).thenReturn(new Department());
        when(procedureRepository.findByDepartmentPublicIdAndNormalizedName(departmentId, "CHEST TUBE INSERTION"))
                .thenReturn(Optional.of(procedure("Chest Tube Insertion", "CHEST TUBE INSERTION", false)));

        assertThatThrownBy(() -> procedureService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reactivate");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(procedureRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> procedureService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatePreservesCodeWhileChangingNameAndDepartment() {
        UUID id = UUID.randomUUID();
        UUID newDepartment = UUID.randomUUID();
        Procedure existing = procedure("Chest Tube Insertion", "CHEST TUBE INSERTION", true);
        existing.setProcedureCode("PRC-0007");
        when(procedureRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentService.getEntityById(newDepartment)).thenReturn(new Department());
        when(procedureRepository.findByDepartmentPublicIdAndNormalizedName(newDepartment, "NEBULIZATION"))
                .thenReturn(Optional.empty());
        when(procedureRepository.saveAndFlush(any(Procedure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcedureResponse response = procedureService.update(id,
                new ProcedureRequest("Nebulization", "Aerosol therapy", newDepartment));

        assertThat(response.procedureCode()).isEqualTo("PRC-0007");
        assertThat(response.name()).isEqualTo("Nebulization");
        assertThat(response.departmentPublicId()).isEqualTo(newDepartment);
    }

    @Test
    void deactivateSetsInactive() {
        UUID id = UUID.randomUUID();
        Procedure existing = procedure("Nebulization", "NEBULIZATION", true);
        when(procedureRepository.findById(id)).thenReturn(Optional.of(existing));
        when(procedureRepository.saveAndFlush(any(Procedure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcedureResponse response = procedureService.deactivate(id);

        assertThat(response.active()).isFalse();
    }

    @Test
    void activateRejectsWhenAnActiveDuplicateExists() {
        UUID id = UUID.randomUUID();
        Procedure inactive = procedure("Nebulization", "NEBULIZATION", false);
        Procedure conflictingActive = procedure("Nebulization", "NEBULIZATION", true);
        when(procedureRepository.findById(id)).thenReturn(Optional.of(inactive));
        lenient().when(procedureRepository.findByDepartmentPublicIdAndNormalizedName(departmentId, "NEBULIZATION"))
                .thenReturn(Optional.of(conflictingActive));

        assertThatThrownBy(() -> procedureService.activate(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        verify(procedureRepository, never()).saveAndFlush(any());
    }
}
