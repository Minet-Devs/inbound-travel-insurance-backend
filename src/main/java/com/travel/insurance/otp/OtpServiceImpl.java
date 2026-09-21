package com.travel.insurance.otp;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.otp.dto.SendOtpRequest;
import com.travel.insurance.otp.dto.VerifyOtpRequest;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final int EXPIRY_MINUTES = 10;

    private final OtpRepository otpRepository;
    private final ServiceProviderService serviceProviderService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void send(SendOtpRequest request) {
        if (!serviceProviderService.exists(request.serviceProviderId())) {
            throw new ResourceNotFoundException("ServiceProvider", request.serviceProviderId());
        }
        findActive(request.email(), request.serviceProviderId()).ifPresent(otpRepository::delete);

        Otp otp = new Otp();
        otp.setEmail(request.email());
        otp.setServiceProviderId(request.serviceProviderId());
        otp.setOtp(generateCode());
        otp.setExpiryTime(Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES));
        otp = otpRepository.save(otp);

        eventPublisher.publishEvent(new OtpGeneratedEvent(otp.getId()));
    }

    @Override
    public void verify(VerifyOtpRequest request) {
        Otp otp = findActive(request.email(), request.serviceProviderId())
                .orElseThrow(() -> new IllegalStateException("Invalid otp"));

        if (Instant.now().isAfter(otp.getExpiryTime())) {
            throw new IllegalStateException("Otp expired");
        }
        if (!otp.getOtp().equals(request.otp())) {
            throw new IllegalStateException("Invalid otp");
        }
        otpRepository.delete(otp);
    }

    private Optional<Otp> findActive(String email, UUID serviceProviderId) {
        return otpRepository.findFirstByEmailAndServiceProviderIdOrderByCreatedDateDesc(email, serviceProviderId);
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
