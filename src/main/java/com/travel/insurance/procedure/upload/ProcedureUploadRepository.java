package com.travel.insurance.procedure.upload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcedureUploadRepository extends JpaRepository<ProcedureUpload, UUID> {
}
