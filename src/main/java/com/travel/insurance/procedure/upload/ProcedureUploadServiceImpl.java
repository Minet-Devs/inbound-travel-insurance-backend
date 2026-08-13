package com.travel.insurance.procedure.upload;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.util.SecurityUtils;
import com.travel.insurance.config.ProcedureUploadProperties;
import com.travel.insurance.department.DepartmentService;
import com.travel.insurance.procedure.Procedure;
import com.travel.insurance.procedure.ProcedureCodeGenerator;
import com.travel.insurance.procedure.ProcedureMapper;
import com.travel.insurance.procedure.ProcedureNameNormalizer;
import com.travel.insurance.procedure.ProcedureNameNormalizer.CleanedName;
import com.travel.insurance.procedure.ProcedureRepository;
import com.travel.insurance.procedure.upload.ProcedureExcelParser.ProcedureExcelRow;
import com.travel.insurance.procedure.upload.dto.ProcedureImportResponse;
import com.travel.insurance.procedure.upload.dto.ProcedureUploadResponse;
import com.travel.insurance.procedure.upload.dto.ProcedureUploadValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcedureUploadServiceImpl implements ProcedureUploadService {

    private final DepartmentService departmentService;
    private final ProcedureExcelParser excelParser;
    private final ProcedureNameNormalizer nameNormalizer;
    private final ProcedureCodeGenerator codeGenerator;
    private final ProcedureMapper procedureMapper;
    private final ProcedureRepository procedureRepository;
    private final ProcedureUploadRepository uploadRepository;
    private final ProcedureUploadRowRepository rowRepository;
    private final ProcedureUploadMapper uploadMapper;
    private final ProcedureUploadWorkbooks workbooks;
    private final ProcedureUploadProperties properties;

    @Override
    public ProcedureUploadValidationResponse validate(UUID departmentPublicId, MultipartFile file) {
        validateDepartment(departmentPublicId);
        validateFile(file);
        List<ProcedureExcelRow> parsed = parse(file);

        ProcedureUpload upload = newUpload(departmentPublicId, file);
        uploadRepository.saveAndFlush(upload);

        List<ProcedureUploadRow> rows = evaluateRows(parsed, departmentPublicId, upload.getId());
        applyValidationCounts(upload, parsed.size(), rows);
        rowRepository.saveAll(rows);
        uploadRepository.save(upload);

        return uploadMapper.toValidationResponse(upload, rows);
    }

    @Override
    public ProcedureImportResponse importUpload(UUID uploadPublicId) {
        ProcedureUpload upload = getUploadEntity(uploadPublicId);
        guardImportEligible(upload);
        upload.setStatus(ProcedureUploadStatus.PROCESSING);
        upload.setProcessingStartTime(Instant.now());
        uploadRepository.saveAndFlush(upload);

        List<ProcedureUploadRow> rows = rowRepository.findByUploadIdOrderByExcelRowNumberAsc(uploadPublicId);
        List<ProcedureUploadRow> candidates = rows.stream().filter(row -> row.getRowStatus() == ProcedureRowStatus.VALID).toList();
        List<ProcedureUploadRow> toCreate = recheckDuplicates(candidates, upload.getDepartmentPublicId());
        createInBatches(toCreate, upload);

        applyImportCounts(upload, rows);
        rowRepository.saveAll(rows);
        uploadRepository.save(upload);
        return uploadMapper.toImportResponse(upload, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedureUploadResponse getUpload(UUID uploadPublicId) {
        return uploadMapper.toUploadResponse(getUploadEntity(uploadPublicId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] template() {
        return workbooks.template();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] errorReport(UUID uploadPublicId) {
        getUploadEntity(uploadPublicId);
        List<ProcedureUploadRow> rows = rowRepository.findByUploadIdAndRowStatusInOrderByExcelRowNumberAsc(
                uploadPublicId, List.of(ProcedureRowStatus.FAILED, ProcedureRowStatus.SKIPPED));
        return workbooks.errorReport(rows);
    }

    private void validateDepartment(UUID departmentPublicId) {
        if (!departmentService.existsActive(departmentPublicId)) {
            throw new IllegalArgumentException("Department is not valid or is inactive: " + departmentPublicId);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("The uploaded file exceeds the maximum size of " + properties.getMaxFileSizeBytes() + " bytes");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("The uploaded file must be a .xlsx workbook");
        }
    }

    private List<ProcedureExcelRow> parse(MultipartFile file) {
        List<ProcedureExcelRow> parsed;
        try {
            parsed = excelParser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded file", e);
        }
        if (parsed.size() > properties.getMaxRows()) {
            throw new IllegalArgumentException("The uploaded file exceeds the maximum of " + properties.getMaxRows());
        }
        return parsed;
    }

    private ProcedureUpload newUpload(UUID departmentPublicId, MultipartFile file) {
        ProcedureUpload upload = new ProcedureUpload();
        upload.setOriginalFilename(file.getOriginalFilename());
        upload.setDepartmentPublicId(departmentPublicId);
        upload.setStatus(ProcedureUploadStatus.VALIDATING);
        upload.setUploadedBy(SecurityUtils.currentUserId().orElse(null));
        return upload;
    }

    private List<ProcedureUploadRow> evaluateRows(List<ProcedureExcelRow> parsed, UUID departmentPublicId, UUID uploadId) {
        List<CleanedRow> cleaned = parsed.stream().map(raw -> new CleanedRow(raw, nameNormalizer.clean(raw.name()))).toList();
        Map<String, Procedure> existing = existingByNormalizedName(departmentPublicId, cleaned.stream()
                .filter(this::eligibleForLookup)
                .map(row -> row.name().normalized())
                .collect(Collectors.toSet()));

        Map<String, Integer> firstRowByNormalized = new HashMap<>();
        List<ProcedureUploadRow> rows = new ArrayList<>(cleaned.size());
        for (CleanedRow row : cleaned) {
            rows.add(evaluateRow(row, uploadId, existing, firstRowByNormalized));
        }
        return rows;
    }

    private ProcedureUploadRow evaluateRow(CleanedRow cleaned, UUID uploadId, Map<String, Procedure> existing, Map<String, Integer> firstRowByNormalized) {
        ProcedureUploadRow row = baseRow(cleaned, uploadId);
        CleanedName name = cleaned.name();
        int excelRow = cleaned.raw().excelRowNumber();

        if (name.isBlank()) {
            return result(row, ProcedureRowStatus.FAILED, ProcedureUploadErrorCode.NAME_REQUIRED, "Procedure name is required.");
        }
        if (name.display().length() > properties.getMaxNameLength()) {
            return result(row, ProcedureRowStatus.FAILED, ProcedureUploadErrorCode.NAME_TOO_LONG, "Procedure name exceeds characters.");
        }
        Integer firstRow = firstRowByNormalized.get(name.normalized());
        if (firstRow != null) {
            return result(row, ProcedureRowStatus.FAILED, ProcedureUploadErrorCode.DUPLICATE_IN_FILE, "Row is duplicated in this file");
        }
        firstRowByNormalized.put(name.normalized(), excelRow);

        Procedure match = existing.get(name.normalized());
        if (match != null && match.isActive()) {
            return result(row, ProcedureRowStatus.SKIPPED, ProcedureUploadErrorCode.ALREADY_EXISTS, "Row already exists.");
        }
        if (match != null) {
            return result(row, ProcedureRowStatus.FAILED, ProcedureUploadErrorCode.INACTIVE_EXISTS, "Row " + excelRow + ": An inactive procedure exists. Reactivate it instead.");
        }
        return result(row, ProcedureRowStatus.VALID, null, null);
    }

    private ProcedureUploadRow baseRow(CleanedRow cleaned, UUID uploadId) {
        ProcedureUploadRow row = new ProcedureUploadRow();
        row.setUploadId(uploadId);
        row.setExcelRowNumber(cleaned.raw().excelRowNumber());
        row.setSubmittedName(cleaned.raw().name());
        row.setSubmittedDescription(blankToNull(cleaned.raw().description()));
        row.setCleanedName(cleaned.name().display());
        row.setNormalizedName(cleaned.name().isBlank() ? null : cleaned.name().normalized());
        return row;
    }

    private boolean eligibleForLookup(CleanedRow row) {
        return !row.name().isBlank() && row.name().display().length() <= properties.getMaxNameLength();
    }

    private void applyValidationCounts(ProcedureUpload upload, int totalRows, List<ProcedureUploadRow> rows) {
        upload.setTotalRows(totalRows);
        upload.setValidRows((int) count(rows, ProcedureRowStatus.VALID));
        upload.setSkippedRows((int) count(rows, ProcedureRowStatus.SKIPPED));
        upload.setFailedRows((int) count(rows, ProcedureRowStatus.FAILED));
        upload.setStatus(upload.getValidRows() > 0
                ? ProcedureUploadStatus.READY_FOR_IMPORT : ProcedureUploadStatus.VALIDATION_FAILED);
    }

    private void guardImportEligible(ProcedureUpload upload) {
        switch (upload.getStatus()) {
            case READY_FOR_IMPORT -> { /* eligible */ }
            case PROCESSING -> throw new IllegalStateException("Upload " + upload.getId() + " is already being processed");
            case COMPLETED, COMPLETED_WITH_ERRORS -> throw new IllegalStateException("Upload " + upload.getId() + " has already been imported");
            default -> throw new IllegalStateException("Upload " + upload.getId()
                    + " is not ready for import (status: " + upload.getStatus() + ")");
        }
    }

    private List<ProcedureUploadRow> recheckDuplicates(List<ProcedureUploadRow> candidates, UUID departmentPublicId) {
        Map<String, Procedure> existing = existingByNormalizedName(departmentPublicId, candidates.stream()
                .map(ProcedureUploadRow::getNormalizedName)
                .collect(Collectors.toSet()));

        List<ProcedureUploadRow> toCreate = new ArrayList<>();
        for (ProcedureUploadRow row : candidates) {
            Procedure match = existing.get(row.getNormalizedName());
            if (match != null && match.isActive()) {
                result(row, ProcedureRowStatus.SKIPPED, ProcedureUploadErrorCode.ALREADY_EXISTS, "Row already exists.");
            } else if (match != null) {
                result(row, ProcedureRowStatus.FAILED, ProcedureUploadErrorCode.INACTIVE_EXISTS, " An inactive procedure exists. Reactivate it instead.");
            } else {
                toCreate.add(row);
            }
        }
        return toCreate;
    }

    private void createInBatches(List<ProcedureUploadRow> toCreate, ProcedureUpload upload) {
        int batchSize = Math.max(1, properties.getBatchSize());
        for (int start = 0; start < toCreate.size(); start += batchSize) {
            List<ProcedureUploadRow> chunk = toCreate.subList(start, Math.min(start + batchSize, toCreate.size()));
            persistChunk(chunk, upload);
        }
    }

    private void persistChunk(List<ProcedureUploadRow> chunk, ProcedureUpload upload) {
        List<Procedure> procedures = chunk.stream().map(row -> procedureMapper.newProcedure(
                new CleanedName(row.getCleanedName(), row.getNormalizedName()),
                blankToNull(row.getSubmittedDescription()),
                upload.getDepartmentPublicId(),
                codeGenerator.next(),
                upload.getId())).toList();
        try {
            List<Procedure> saved = procedureRepository.saveAll(procedures);
            procedureRepository.flush();
            for (int i = 0; i < chunk.size(); i++) {
                ProcedureUploadRow row = result(chunk.get(i), ProcedureRowStatus.CREATED, null, null);
                row.setCreatedProcedurePublicId(saved.get(i).getId());
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("A concurrent change introduced a duplicate procedure; please re-validate and import again.");
        }
    }

    private void applyImportCounts(ProcedureUpload upload, List<ProcedureUploadRow> rows) {
        upload.setCreatedRows((int) count(rows, ProcedureRowStatus.CREATED));
        upload.setSkippedRows((int) count(rows, ProcedureRowStatus.SKIPPED));
        upload.setFailedRows((int) count(rows, ProcedureRowStatus.FAILED));
        upload.setStatus(upload.getFailedRows() > 0 || upload.getSkippedRows() > 0 ? ProcedureUploadStatus.COMPLETED_WITH_ERRORS : ProcedureUploadStatus.COMPLETED);
        upload.setCompletionTime(Instant.now());
    }

    private Map<String, Procedure> existingByNormalizedName(UUID departmentPublicId, Set<String> normalizedNames) {
        if (normalizedNames.isEmpty()) {
            return Map.of();
        }
        return procedureRepository.findByDepartmentPublicIdAndNormalizedNameIn(departmentPublicId, normalizedNames).stream()
                .collect(Collectors.toMap(Procedure::getNormalizedName, Function.identity(), (a, b) -> a));
    }

    private ProcedureUploadRow result(ProcedureUploadRow row, ProcedureRowStatus status, ProcedureUploadErrorCode code, String message) {
        row.setRowStatus(status);
        row.setErrorCode(code == null ? null : code.name());
        row.setErrorMessage(message);
        return row;
    }

    private long count(List<ProcedureUploadRow> rows, ProcedureRowStatus status) {
        return rows.stream().filter(row -> row.getRowStatus() == status).count();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProcedureUpload getUploadEntity(UUID uploadPublicId) {
        return uploadRepository.findById(uploadPublicId).orElseThrow(() -> new ResourceNotFoundException("ProcedureUpload", uploadPublicId));
    }

    private record CleanedRow(ProcedureExcelRow raw, CleanedName name) {
    }
}
