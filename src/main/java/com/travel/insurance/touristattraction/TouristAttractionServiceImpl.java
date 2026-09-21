package com.travel.insurance.touristattraction;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.touristattraction.dto.TouristAttractionRequest;
import com.travel.insurance.touristattraction.dto.TouristAttractionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TouristAttractionServiceImpl implements TouristAttractionService {

    /** Below this length a typo-tolerant match is too loose to be meaningful. */
    private static final int MIN_FUZZY_QUERY_LENGTH = 4;

    private final TouristAttractionRepository touristAttractionRepository;
    private final TouristAttractionMapper touristAttractionMapper;

    @Override
    public TouristAttractionResponse create(TouristAttractionRequest request) {
        if (touristAttractionRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new IllegalStateException("Tourist attraction already exists: " + request.name());
        }
        TouristAttraction attraction = touristAttractionMapper.toEntity(request);
        return touristAttractionMapper.toResponse(touristAttractionRepository.save(attraction));
    }

    @Override
    @Transactional(readOnly = true)
    public TouristAttractionResponse getById(UUID id) {
        return touristAttractionMapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TouristAttractionResponse> list(Pageable pageable) {
        return touristAttractionRepository.findAll(pageable).map(touristAttractionMapper::toResponse);
    }

    @Override
    public TouristAttractionResponse update(UUID id, TouristAttractionRequest request) {
        TouristAttraction attraction = getEntity(id);
        if (touristAttractionRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new IllegalStateException("Tourist attraction already exists: " + request.name());
        }
        touristAttractionMapper.updateEntity(attraction, request);
        return touristAttractionMapper.toResponse(touristAttractionRepository.save(attraction));
    }

    @Override
    public void delete(UUID id) {
        touristAttractionRepository.delete(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TouristAttractionResponse> searchByName(String query, int limit) {
        String q = NameMatcher.normalise(query);
        if (q.isEmpty()) {
            return List.of();
        }
        int max = Math.max(1, limit);
        List<TouristAttraction> all = touristAttractionRepository.findAll(Sort.by("name").ascending());

        List<TouristAttraction> matches = all.stream()
                .filter(a -> NameMatcher.normalise(a.getName()).equals(q))
                .toList();
        if (matches.isEmpty()) {
            matches = all.stream()
                    .filter(a -> NameMatcher.normalise(a.getName()).contains(q))
                    .toList();
        }
        if (matches.isEmpty() && q.length() >= MIN_FUZZY_QUERY_LENGTH) {
            int threshold = Math.max(1, q.length() / 4);
            matches = all.stream()
                    .filter(a -> NameMatcher.substringDistance(q, NameMatcher.normalise(a.getName())) <= threshold)
                    .sorted(Comparator.comparingInt(
                            a -> NameMatcher.substringDistance(q, NameMatcher.normalise(a.getName()))))
                    .toList();
        }
        return matches.stream()
                .limit(max)
                .map(touristAttractionMapper::toResponse)
                .toList();
    }

    private TouristAttraction getEntity(UUID id) {
        return touristAttractionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TouristAttraction", id));
    }
}
