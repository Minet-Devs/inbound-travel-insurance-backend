package com.travel.insurance.serviceprovider;

import com.travel.insurance.serviceprovider.dto.ServiceProviderNearbyResponse;
import com.travel.insurance.serviceprovider.dto.ServiceProviderRequest;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-providers")
@RequiredArgsConstructor
public class ServiceProviderController {

    private final ServiceProviderService serviceProviderService;

    @PostMapping
    public ResponseEntity<ServiceProviderResponse> create(@Valid @RequestBody ServiceProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceProviderService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceProviderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceProviderService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ServiceProviderResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(serviceProviderService.list(pageable));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<ServiceProviderNearbyResponse>> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") BigDecimal lng,
            @RequestParam @DecimalMin(value = "0", inclusive = false) double radiusKm) {
        return ResponseEntity.ok(serviceProviderService.findNearby(lat, lng, radiusKm));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceProviderResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody ServiceProviderRequest request) {
        return ResponseEntity.ok(serviceProviderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceProviderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
