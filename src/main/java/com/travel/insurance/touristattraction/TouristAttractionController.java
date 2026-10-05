package com.travel.insurance.touristattraction;

import com.travel.insurance.touristattraction.dto.TouristAttractionRequest;
import com.travel.insurance.touristattraction.dto.TouristAttractionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tourist-attractions")
@RequiredArgsConstructor
public class TouristAttractionController {

    private final TouristAttractionService touristAttractionService;

    @PostMapping
    public ResponseEntity<TouristAttractionResponse> create(@Valid @RequestBody TouristAttractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(touristAttractionService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TouristAttractionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(touristAttractionService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<TouristAttractionResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(touristAttractionService.list(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TouristAttractionResponse> update(@PathVariable UUID id,
                                                            @Valid @RequestBody TouristAttractionRequest request) {
        return ResponseEntity.ok(touristAttractionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        touristAttractionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
