package com.travel.insurance.minet;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visitors/minet-sync")
@RequiredArgsConstructor
public class MinetController {

    private final MinetVisitorSyncService minetVisitorSyncService;

    @PostMapping("/{passportNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public MinetSyncResponse sync(@PathVariable String passportNumber) {
        return minetVisitorSyncService.syncByPassportNumber(passportNumber);
    }
}
