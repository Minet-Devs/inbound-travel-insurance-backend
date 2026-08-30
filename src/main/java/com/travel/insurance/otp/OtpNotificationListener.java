package com.travel.insurance.otp;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.common.email.SmtpCredentials;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Sends the OTP email after the generating transaction commits, mirroring
 * {@code notification.VisitorActivatedNotificationListener}'s AFTER_COMMIT
 * pattern: an SMTP call must never be able to roll back the OTP row being
 * persisted. The insurer mailbox is resolved from a fixed organization id
 * rather than derived from the request, per the current spec.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OtpNotificationListener {

    private static final UUID OTP_SENDER_ORGANIZATION_ID = UUID.fromString("db705c1e-05e8-48c6-b0ea-62237256e7b3");

    private final OtpRepository otpRepository;
    private final InsurerService insurerService;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOtpGenerated(OtpGeneratedEvent event) {
        try {
            sendOtpEmail(event.otpId());
        } catch (Exception ex) {
            log.error("Failed to send OTP email for otp {}: {}", event.otpId(), ex.getMessage(), ex);
        }
    }

    private void sendOtpEmail(UUID otpId) {
        Otp otp = otpRepository.findById(otpId)
                .orElseThrow(() -> new IllegalStateException("Otp not found: " + otpId));

        MailSettings mailSettings = resolveMailSettings();
        emailService.send(
                mailSettings.credentials(),
                mailSettings.from(),
                otp.getEmail(),
                "Your verification code",
                "<p>Your verification code is <strong>" + otp.getOtp() + "</strong>.</p>"
                        + "<p>This code expires in 10 minutes.</p>");
        log.info("Sent OTP email to {} for service provider {}", otp.getEmail(), otp.getServiceProviderId());
    }

    private MailSettings resolveMailSettings() {
        Insurer insurer = insurerService.findIdByOrganizationId(OTP_SENDER_ORGANIZATION_ID)
                .map(insurerService::getEntityById)
                .orElse(null);
        boolean fullyConfigured = insurer != null
                && isNotBlank(insurer.getHost())
                && insurer.getPort() != null
                && isNotBlank(insurer.getNotificationEmail())
                && isNotBlank(insurer.getNotificationEmailPassword());
        if (fullyConfigured) {
            return new MailSettings(
                    insurer.getNotificationEmail(),
                    new SmtpCredentials(insurer.getHost(), insurer.getPort(),
                            insurer.getNotificationEmail(), insurer.getNotificationEmailPassword()));
        }
        return new MailSettings(mailProperties.getFrom(), null);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record MailSettings(String from, SmtpCredentials credentials) {
    }
}
