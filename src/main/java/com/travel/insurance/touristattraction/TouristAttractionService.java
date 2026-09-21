package com.travel.insurance.touristattraction;

import com.travel.insurance.touristattraction.dto.TouristAttractionRequest;
import com.travel.insurance.touristattraction.dto.TouristAttractionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TouristAttractionService {

    TouristAttractionResponse create(TouristAttractionRequest request);

    TouristAttractionResponse getById(UUID id);

    Page<TouristAttractionResponse> list(Pageable pageable);

    TouristAttractionResponse update(UUID id, TouristAttractionRequest request);

    void delete(UUID id);

    /**
     * Finds the attractions closest to {@code query}, best match first, at most {@code limit}.
     * A case-insensitive exact name wins outright; otherwise names containing the query;
     * otherwise names within a small edit distance of it (typo tolerance). Blank query
     * returns an empty list.
     */
    List<TouristAttractionResponse> searchByName(String query, int limit);
}
