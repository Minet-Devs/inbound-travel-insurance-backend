package com.travel.insurance.insurer;

import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InsurerService {

    InsurerResponse create(InsurerRequest request);

    InsurerResponse getById(UUID id);

    Page<InsurerResponse> list(Pageable pageable);

    InsurerResponse update(UUID id, InsurerRequest request);

    void delete(UUID id);

    boolean exists(UUID id);
}
