package com.travel.insurance.procedure.upload;

import com.travel.insurance.procedure.upload.dto.ProcedureImportResponse;
import com.travel.insurance.procedure.upload.dto.ProcedureUploadResponse;
import com.travel.insurance.procedure.upload.dto.ProcedureUploadValidationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProcedureUploadService {

    ProcedureUploadValidationResponse validate(MultipartFile file);

    ProcedureImportResponse importUpload(UUID uploadPublicId);

    ProcedureUploadResponse getUpload(UUID uploadPublicId);

    byte[] template();

    byte[] errorReport(UUID uploadPublicId);
}
