package com.travel.insurance.mobileauth;

import com.travel.insurance.mobileauth.dto.RequestVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VerifyVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VisitorTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    @PostMapping("/otp/request")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody RequestVisitorOtpRequest request) {
        mobileAuthService.requestOtp(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<VisitorTokenResponse> verifyOtp(@Valid @RequestBody VerifyVisitorOtpRequest request) {
        return ResponseEntity.ok(mobileAuthService.verifyOtp(request));
    }
}
