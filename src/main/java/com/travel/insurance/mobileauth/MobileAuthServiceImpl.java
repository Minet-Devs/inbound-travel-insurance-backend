package com.travel.insurance.mobileauth;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.mobileauth.dto.RequestVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VerifyVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VisitorTokenResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MobileAuthServiceImpl implements MobileAuthService {

    private static final int EXPIRY_MINUTES = 10;
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final VisitorOtpRepository visitorOtpRepository;
    private final VisitorService visitorService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void requestOtp(RequestVisitorOtpRequest request) {
        if (visitorService.findByEmail(request.email()).isEmpty()) {
            // No enumeration: silently no-op for unknown emails, still respond 202.
            return;
        }

        Optional<VisitorOtp> existing = visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(request.email());
        if (existing.isPresent()) {
            VisitorOtp previous = existing.get();
            if (Instant.now().isBefore(previous.getCreatedDate().plus(RESEND_COOLDOWN))) {
                throw new IllegalStateException("Please wait before requesting another code");
            }
            visitorOtpRepository.delete(previous);
        }

        VisitorOtp otp = new VisitorOtp();
        otp.setEmail(request.email());
        otp.setOtp(generateCode());
        otp.setExpiryTime(Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES));
        otp = visitorOtpRepository.save(otp);

        eventPublisher.publishEvent(new VisitorOtpGeneratedEvent(otp.getId()));
    }

    @Override
    public VisitorTokenResponse verifyOtp(VerifyVisitorOtpRequest request) {
        VisitorOtp otp = visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(request.email())
                .filter(found -> Instant.now().isBefore(found.getExpiryTime()))
                .filter(found -> found.getOtp().equals(request.otp()))
                .orElseThrow(() -> new IllegalStateException("Invalid or expired code"));

        Visitor visitor = visitorService.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Invalid or expired code"));

        visitorOtpRepository.delete(otp);

        return VisitorTokenResponse.bearer(
                jwtTokenProvider.createVisitorAccessToken(visitor),
                jwtTokenProvider.createVisitorRefreshToken(visitor),
                jwtTokenProvider.accessTokenTtlSeconds());
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
