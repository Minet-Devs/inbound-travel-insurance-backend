package com.travel.insurance.insurer;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InsurerServiceImpl implements InsurerService {

    private final InsurerRepository insurerRepository;
    private final InsurerMapper insurerMapper;

    @Override
    public InsurerResponse create(InsurerRequest request) {
        if (insurerRepository.existsByName(request.name())) {
            throw new IllegalStateException("Insurer already exists: " + request.name());
        }
        return insurerMapper.toResponse(insurerRepository.save(insurerMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public InsurerResponse getById(UUID id) {
        return insurerMapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InsurerResponse> list(Pageable pageable) {
        return insurerRepository.findAll(pageable).map(insurerMapper::toResponse);
    }

    @Override
    public InsurerResponse update(UUID id, InsurerRequest request) {
        Insurer insurer = getEntity(id);
        insurerMapper.updateEntity(insurer, request);
        return insurerMapper.toResponse(insurerRepository.save(insurer));
    }

    @Override
    public void delete(UUID id) {
        insurerRepository.delete(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return insurerRepository.existsById(id);
    }

    private Insurer getEntity(UUID id) {
        return insurerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurer", id));
    }
}
