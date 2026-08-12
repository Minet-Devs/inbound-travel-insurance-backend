package com.travel.insurance.icd11;

import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.icd11.dto.Icd11ImportResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface Icd11CodeService {

    Icd11ImportResult importFromExcel(MultipartFile file);

    Page<Icd11CodeResponse> search(String query, Pageable pageable);

    Icd11CodeResponse getByCode(String code);
}
