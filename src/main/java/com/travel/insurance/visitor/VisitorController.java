package com.travel.insurance.visitor;

import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;

    @PostMapping
    public ResponseEntity<VisitorResponse> create(@Valid @RequestBody VisitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitorService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisitorResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(visitorService.getById(id));
    }

    @GetMapping("/by-policy")
    public ResponseEntity<VisitorResponse> getByPolicyId(@RequestParam UUID policyId) {
        return ResponseEntity.ok(visitorService.getByPolicyId(policyId));
    }

    @GetMapping
    public ResponseEntity<Page<VisitorResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(visitorService.list(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VisitorResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody VisitorRequest request) {
        return ResponseEntity.ok(visitorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        visitorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
