package com.travel.insurance.preauthorization.upload;

import com.travel.insurance.preauthorization.dto.PreauthorizationUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PreauthorizationUploadService {

    /**
     * Imports offline-approved pre-authorizations from an {@code .xlsx}. Each valid row is created
     * as an {@code APPROVED} pre-authorization; failing rows are skipped and reported per row.
     */
    PreauthorizationUploadResponse upload(MultipartFile file);
}
