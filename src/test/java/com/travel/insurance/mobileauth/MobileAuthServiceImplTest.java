package com.travel.insurance.mobileauth;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.mobileauth.dto.RequestVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VerifyVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VisitorTokenResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileAuthServiceImplTest {

    @Mock
    private VisitorOtpRepository visitorOtpRepository;

    @Mock
    private VisitorService visitorService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MobileAuthServiceImpl mobileAuthService;

    private final String email = "visitor@example.com";

    @BeforeEach
    void setUp() {
        mobileAuthService = new MobileAuthServiceImpl(
                visitorOtpRepository, visitorService, jwtTokenProvider, eventPublisher);
    }

    @Test
    void requestOtpSilentlyNoOpsForUnknownEmail() {
        when(visitorService.findByEmail(email)).thenReturn(Optional.empty());

        mobileAuthService.requestOtp(new RequestVisitorOtpRequest(email));

        verify(visitorOtpRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void requestOtpGeneratesAndSavesOtp() {
        when(visitorService.findByEmail(email)).thenReturn(Optional.of(new Visitor()));
        when(visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(email)).thenReturn(Optional.empty());
        when(visitorOtpRepository.save(any(VisitorOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mobileAuthService.requestOtp(new RequestVisitorOtpRequest(email));

        verify(visitorOtpRepository).save(any(VisitorOtp.class));
        verify(eventPublisher).publishEvent(any(VisitorOtpGeneratedEvent.class));
    }

    @Test
    void requestOtpRejectsResendWithinCooldown() {
        VisitorOtp existing = new VisitorOtp();
        existing.setEmail(email);
        existing.setCreatedDate(Instant.now());
        when(visitorService.findByEmail(email)).thenReturn(Optional.of(new Visitor()));
        when(visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(email)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> mobileAuthService.requestOtp(new RequestVisitorOtpRequest(email)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wait");
        verify(visitorOtpRepository, never()).delete(any());
        verify(visitorOtpRepository, never()).save(any());
    }

    @Test
    void requestOtpInvalidatesExistingOtpAfterCooldown() {
        VisitorOtp existing = new VisitorOtp();
        existing.setEmail(email);
        existing.setCreatedDate(Instant.now().minus(2, ChronoUnit.MINUTES));
        when(visitorService.findByEmail(email)).thenReturn(Optional.of(new Visitor()));
        when(visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(email)).thenReturn(Optional.of(existing));
        when(visitorOtpRepository.save(any(VisitorOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mobileAuthService.requestOtp(new RequestVisitorOtpRequest(email));

        verify(visitorOtpRepository).delete(existing);
        verify(visitorOtpRepository).save(any(VisitorOtp.class));
    }

    @Test
    void verifyOtpThrowsUniformMessageWhenNoOtpFound() {
        when(visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mobileAuthService.verifyOtp(new VerifyVisitorOtpRequest(email, "123456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid or expired code");
    }

    @Test
    void verifyOtpThrowsUniformMessageWhenExpired() {
        VisitorOtp otp = new VisitorOtp();
        otp.setEmail(email);
        otp.setOtp("123456");
        otp.setExpiryTime(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(email)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> mobileAuthService.verifyOtp(new VerifyVisitorOtpRequest(email, "123456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid or expired code");
        verify(visitorOtpRepository, never()).delete(any());
    }

    @Test
    void verifyOtpThrowsUniformMessageWhenCodeDoesNotMatch() {
        VisitorOtp otp = new VisitorOtp();
        otp.setEmail(email);
        otp.setOtp("123456");
        otp.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(email)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> mobileAuthService.verifyOtp(new VerifyVisitorOtpRequest(email, "999999")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid or expired code");
        verify(visitorOtpRepository, never()).delete(any());
    }

    @Test
    void verifyOtpThrowsUniformMessageWhenVisitorGoneButOtpValid() {
        VisitorOtp otp = new VisitorOtp();
        otp.setEmail(email);
        otp.setOtp("123456");
        otp.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(email)).thenReturn(Optional.of(otp));
        when(visitorService.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mobileAuthService.verifyOtp(new VerifyVisitorOtpRequest(email, "123456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid or expired code");
        verify(visitorOtpRepository, never()).delete(any());
    }

    @Test
    void verifyOtpIssuesTokenOnSuccess() {
        VisitorOtp otp = new VisitorOtp();
        otp.setEmail(email);
        otp.setOtp("123456");
        otp.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        Visitor visitor = new Visitor();
        UUID visitorId = UUID.randomUUID();
        visitor.setId(visitorId);
        when(visitorOtpRepository.findFirstByEmailOrderByCreatedDateDesc(email)).thenReturn(Optional.of(otp));
        when(visitorService.findByEmail(email)).thenReturn(Optional.of(visitor));
        when(jwtTokenProvider.createVisitorAccessToken(visitor)).thenReturn("access-token");
        when(jwtTokenProvider.createVisitorRefreshToken(visitor)).thenReturn("refresh-token");
        when(jwtTokenProvider.accessTokenTtlSeconds()).thenReturn(900L);

        VisitorTokenResponse response = mobileAuthService.verifyOtp(new VerifyVisitorOtpRequest(email, "123456"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
        verify(visitorOtpRepository).delete(otp);
    }
}
