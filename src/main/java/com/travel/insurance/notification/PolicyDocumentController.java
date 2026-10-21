package com.travel.insurance.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
public class PolicyDocumentController {

    private final VisitorActivatedNotificationListener documentGenerator;

    @GetMapping("/{id}/policy-documents")
    public ResponseEntity<byte[]> getPolicyDocuments(@PathVariable UUID id) {
        byte[] pdf = documentGenerator.generateCertificateAndReceiptPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("Insurance Policy.pdf").build().toString())
                .body(pdf);
    }
}
