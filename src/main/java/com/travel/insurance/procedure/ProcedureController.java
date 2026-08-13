package com.travel.insurance.procedure;

import com.travel.insurance.procedure.dto.ProcedureRequest;
import com.travel.insurance.procedure.dto.ProcedureResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/procedures")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureService procedureService;

    @PostMapping
    public ResponseEntity<ProcedureResponse> create(@Valid @RequestBody ProcedureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procedureService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedureResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(procedureService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProcedureResponse>> list(@RequestParam(required = false) String search,
                                                        @RequestParam(required = false) UUID departmentPublicId,
                                                        @RequestParam(required = false) Boolean active,
                                                        @Parameter(hidden = true) Pageable pageable) {
        return ResponseEntity.ok(procedureService.list(search, departmentPublicId, active, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcedureResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody ProcedureRequest request) {
        return ResponseEntity.ok(procedureService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProcedureResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(procedureService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProcedureResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(procedureService.deactivate(id));
    }
}
