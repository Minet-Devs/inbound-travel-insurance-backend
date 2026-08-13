package com.travel.insurance.procedure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcedureRepository extends JpaRepository<Procedure, UUID>, JpaSpecificationExecutor<Procedure> {

    @Query(value = "select nextval('procedure_code_seq')", nativeQuery = true)
    long nextProcedureCodeValue();

    Optional<Procedure> findByProcedureCode(String procedureCode);

    Optional<Procedure> findByDepartmentPublicIdAndNormalizedName(UUID departmentPublicId, String normalizedName);

    List<Procedure> findByDepartmentPublicIdAndNormalizedNameIn(UUID departmentPublicId, Collection<String> normalizedNames);
}
