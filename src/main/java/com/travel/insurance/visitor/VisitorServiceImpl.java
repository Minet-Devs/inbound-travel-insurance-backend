package com.travel.insurance.visitor;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitorServiceImpl implements VisitorService {

    private final VisitorRepository visitorRepository;
    private final VisitorMapper visitorMapper;
    private final PolicyService policyService;

    @Override
    public VisitorResponse create(VisitorRequest request) {
        policyService.getEntityById(request.policyId());
        if (visitorRepository.existsByPolicyId(request.policyId())) {
            throw new IllegalStateException(
                    "Policy already has a visitor: " + request.policyId());
        }
        if (visitorRepository.existsByPassportNumberIgnoreCase(request.passportNumber())) {
            throw new IllegalStateException(
                    "Visitor already exists with passport number: " + request.passportNumber());
        }
        return visitorMapper.toResponse(visitorRepository.save(visitorMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public VisitorResponse getById(UUID id) {
        return visitorMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public VisitorResponse getByPolicyId(UUID policyId) {
        return visitorRepository.findByPolicyId(policyId)
                .map(visitorMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", policyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VisitorResponse> list(Pageable pageable) {
        return visitorRepository.findAll(pageable).map(visitorMapper::toResponse);
    }

    @Override
    public VisitorResponse update(UUID id, VisitorRequest request) {
        Visitor visitor = getEntityById(id);
        policyService.getEntityById(request.policyId());
        if (visitorRepository.existsByPolicyIdAndIdNot(request.policyId(), id)) {
            throw new IllegalStateException(
                    "Policy already has a visitor: " + request.policyId());
        }
        if (visitorRepository.existsByPassportNumberIgnoreCaseAndIdNot(request.passportNumber(), id)) {
            throw new IllegalStateException(
                    "Visitor already exists with passport number: " + request.passportNumber());
        }
        visitorMapper.updateEntity(visitor, request);
        return visitorMapper.toResponse(visitorRepository.save(visitor));
    }

    @Override
    public void delete(UUID id) {
        visitorRepository.delete(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Visitor getEntityById(UUID id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", id));
    }
}
