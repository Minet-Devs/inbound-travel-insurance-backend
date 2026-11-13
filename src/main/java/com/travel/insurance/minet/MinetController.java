package com.travel.insurance.minet;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
public class MinetController {

    private final MinetVisitorSyncService minetVisitorSyncService;

    @PostMapping("/minet-sync/{passportNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public MinetSyncResponse sync(@PathVariable String passportNumber) {
        return minetVisitorSyncService.syncByPassportNumber(passportNumber);
    }

    /** Returns the member with this certificate serial number in the Minet member format. */
    @GetMapping("/minet/certificate/{certificateNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public MinetVisitorRequest memberByCertificate(@PathVariable String certificateNumber) {
        return minetVisitorSyncService.getMemberByCertificateNumber(certificateNumber);
    }
}
