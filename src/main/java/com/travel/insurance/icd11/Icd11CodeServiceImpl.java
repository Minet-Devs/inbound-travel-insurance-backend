package com.travel.insurance.icd11;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.icd11.Icd11ExcelParser.Icd11Row;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.icd11.dto.Icd11ImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class Icd11CodeServiceImpl implements Icd11CodeService {

    private final Icd11CodeRepository icd11CodeRepository;
    private final Icd11CodeMapper icd11CodeMapper;
    private final Icd11ExcelParser icd11ExcelParser;

    @Override
    public Icd11ImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }

        List<Icd11Row> rows;
        try {
            rows = icd11ExcelParser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded file", e);
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        for (Icd11Row row : rows) {
            if (row.code().isBlank() || row.title().isBlank()) {
                skipped++;
                continue;
            }
            Icd11Code entity = icd11CodeRepository.findByCode(row.code()).orElse(null);
            if (entity == null) {
                entity = new Icd11Code();
                entity.setCode(row.code());
                entity.setTitle(row.title());
                icd11CodeRepository.save(entity);
                inserted++;
            } else {
                entity.setTitle(row.title());
                icd11CodeRepository.save(entity);
                updated++;
            }
        }
        return new Icd11ImportResult(rows.size(), inserted, updated, skipped);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Icd11CodeResponse> search(String query, Pageable pageable) {
        Page<Icd11Code> results = (query == null || query.isBlank())
                ? icd11CodeRepository.findAll(pageable)
                : icd11CodeRepository.findByCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
                        query, query, pageable);
        return results.map(icd11CodeMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Icd11CodeResponse> searchByTitle(String title, Pageable pageable) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return icd11CodeRepository.findByTitleContainingIgnoreCase(title, pageable)
                .map(icd11CodeMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Icd11CodeResponse getByCode(String code) {
        return icd11CodeRepository.findByCode(code)
                .map(icd11CodeMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Icd11Code", code));
    }

    @Override
    @Transactional(readOnly = true)
    public Icd11Code getEntityById(UUID id) {
        return icd11CodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Icd11Code", id));
    }
}
