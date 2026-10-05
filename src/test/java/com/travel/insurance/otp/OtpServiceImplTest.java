package com.travel.insurance.otp;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.otp.dto.SendOtpRequest;
import com.travel.insurance.otp.dto.VerifyOtpRequest;
import com.travel.insurance.serviceprovider.ServiceProviderService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private ServiceProviderService serviceProviderService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OtpServiceImpl otpService;

    private final UUID serviceProviderId = UUID.randomUUID();
    private final String email = "visitor@example.com";

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(otpRepository, serviceProviderService, eventPublisher);
    }

    @Test
    void sendRejectsUnknownServiceProvider() {
        when(serviceProviderService.exists(serviceProviderId)).thenReturn(false);

        assertThatThrownBy(() -> otpService.send(new SendOtpRequest(email, serviceProviderId)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(otpRepository, never()).save(any());
    }

    @Test
    void sendGeneratesAndSavesOtp() {
        when(serviceProviderService.exists(serviceProviderId)).thenReturn(true);
        when(otpRepository.findFirstByEmailAndServiceProviderIdOrderByCreatedDateDesc(email, serviceProviderId))
                .thenReturn(Optional.empty());
        when(otpRepository.save(any(Otp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        otpService.send(new SendOtpRequest(email, serviceProviderId));

        verify(otpRepository).save(any(Otp.class));
        verify(eventPublisher).publishEvent(any(OtpGeneratedEvent.class));
    }

    @Test
    void sendInvalidatesExistingOtp() {
        Otp existing = new Otp();
        existing.setEmail(email);
        existing.setServiceProviderId(serviceProviderId);
        when(serviceProviderService.exists(serviceProviderId)).thenReturn(true);
        when(otpRepository.findFirstByEmailAndServiceProviderIdOrderByCreatedDateDesc(email, serviceProviderId))
                .thenReturn(Optional.of(existing));
        when(otpRepository.save(any(Otp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        otpService.send(new SendOtpRequest(email, serviceProviderId));

        verify(otpRepository).delete(existing);
    }

    @Test
    void verifyThrowsWhenNoOtpFound() {
        when(otpRepository.findFirstByEmailAndServiceProviderIdOrderByCreatedDateDesc(email, serviceProviderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verify(new VerifyOtpRequest(email, serviceProviderId, "123456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid otp");
    }

    @Test
    void verifyThrowsWhenExpired() {
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setServiceProviderId(serviceProviderId);
        otp.setOtp("123456");
        otp.setExpiryTime(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(otpRepository.findFirstByEmailAndServiceProviderIdOrderByCreatedDateDesc(email, serviceProviderId))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verify(new VerifyOtpRequest(email, serviceProviderId, "123456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Otp expired");
        verify(otpRepository, never()).delete(any());
    }

    @Test
    void verifyThrowsWhenCodeDoesNotMatch() {
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setServiceProviderId(serviceProviderId);
        otp.setOtp("123456");
        otp.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpRepository.findFirstByEmailAndServiceProviderIdOrderByCreatedDateDesc(email, serviceProviderId))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verify(new VerifyOtpRequest(email, serviceProviderId, "999999")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid otp");
        verify(otpRepository, never()).delete(any());
    }

    @Test
    void verifyInvalidatesOtpOnSuccess() {
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setServiceProviderId(serviceProviderId);
        otp.setOtp("123456");
        otp.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpRepository.findFirstByEmailAndServiceProviderIdOrderByCreatedDateDesc(email, serviceProviderId))
                .thenReturn(Optional.of(otp));

        otpService.verify(new VerifyOtpRequest(email, serviceProviderId, "123456"));

        verify(otpRepository).delete(otp);
    }
}
