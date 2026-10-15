package com.travel.insurance.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visitors/by-passport")
@RequiredArgsConstructor
public class ActivationEmailController {

    private final VisitorActivatedNotificationListener activationNotificationListener;

    @PostMapping("/resend-activation-email")
    public ResponseEntity<ActivationEmailResendResponse> resendActivationEmail(
            @RequestParam String passportNumber) {
        UUID visitorId = activationNotificationListener.resendActivationEmailByPassportNumber(passportNumber);
        return ResponseEntity.ok(new ActivationEmailResendResponse(visitorId, "Activation email sent"));
    }

    public record ActivationEmailResendResponse(UUID visitorId, String message) {
    }
}
