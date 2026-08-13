package com.travel.insurance.procedure;

import com.travel.insurance.procedure.ProcedureNameNormalizer.CleanedName;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProcedureMapper {

    public Procedure newProcedure(CleanedName name, String description, UUID departmentPublicId, String procedureCode, UUID uploadBatchPublicId) {
        Procedure procedure = new Procedure();
        procedure.setProcedureCode(procedureCode);
        procedure.setName(name.display());
        procedure.setNormalizedName(name.normalized());
        procedure.setDescription(description);
        procedure.setDepartmentPublicId(departmentPublicId);
        procedure.setUploadBatchPublicId(uploadBatchPublicId);
        procedure.setActive(true);
        return procedure;
    }

    public ProcedureResponse toResponse(Procedure procedure) {
        return new ProcedureResponse(
                procedure.getId(),
                procedure.getProcedureCode(),
                procedure.getName(),
                procedure.getDescription(),
                procedure.getDepartmentPublicId(),
                procedure.isActive(),
                procedure.getUploadBatchPublicId(),
                procedure.getCreatedDate(),
                procedure.getUpdatedDate()
        );
    }
}
