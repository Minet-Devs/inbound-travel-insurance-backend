package com.travel.insurance.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/welcome-pack")
@RequiredArgsConstructor
public class WelcomePackController {

    private final VisitorActivatedNotificationListener documentGenerator;

    @GetMapping
    public ResponseEntity<byte[]> getWelcomePack() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("Inbound-Travel-Health-Welcome-Pack.pdf").build().toString())
                .body(documentGenerator.getWelcomePackPdf());
    }
}
