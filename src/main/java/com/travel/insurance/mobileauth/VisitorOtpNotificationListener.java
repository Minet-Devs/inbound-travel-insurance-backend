package com.travel.insurance.mobileauth;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.common.email.SmtpCredentials;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.organization.Organization;
import com.travel.insurance.organization.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Sends the visitor login OTP email after the generating transaction commits,
 * mirroring {@code otp.OtpNotificationListener}'s AFTER_COMMIT pattern: an
 * SMTP call must never be able to roll back the OTP row being persisted.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VisitorOtpNotificationListener {

    private static final UUID OTP_SENDER_ORGANIZATION_ID = UUID.fromString("db705c1e-05e8-48c6-b0ea-62237256e7b3");

    private final VisitorOtpRepository visitorOtpRepository;
    private final OrganizationService organizationService;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVisitorOtpGenerated(VisitorOtpGeneratedEvent event) {
        try {
            sendOtpEmail(event.visitorOtpId());
        } catch (Exception ex) {
            log.error("Failed to send visitor login OTP email for otp {}: {}",
                    event.visitorOtpId(), ex.getMessage(), ex);
        }
    }

    private void sendOtpEmail(UUID otpId) {
        VisitorOtp otp = visitorOtpRepository.findById(otpId)
                .orElseThrow(() -> new IllegalStateException("VisitorOtp not found: " + otpId));

        MailSettings mailSettings = resolveMailSettings();
        emailService.send(
                mailSettings.credentials(),
                mailSettings.from(),
                otp.getEmail(),
                "Your mobile login code",
                "<p>Your login code is <strong>" + otp.getOtp() + "</strong>.</p>"
                        + "<p>This code expires in 10 minutes.</p>");
        log.info("Sent visitor login OTP email to {}", otp.getEmail());
    }

    private MailSettings resolveMailSettings() {
        Organization organization;
        try {
            organization = organizationService.getEntityById(OTP_SENDER_ORGANIZATION_ID);
        } catch (ResourceNotFoundException ex) {
            return new MailSettings(mailProperties.getFrom(), null);
        }
        boolean fullyConfigured = isNotBlank(organization.getHost())
                && organization.getPort() != null
                && isNotBlank(organization.getNotificationEmail())
                && isNotBlank(organization.getNotificationEmailPassword());
        if (fullyConfigured) {
            return new MailSettings(
                    organization.getNotificationEmail(),
                    new SmtpCredentials(organization.getHost(), organization.getPort(),
                            organization.getNotificationEmail(), organization.getNotificationEmailPassword()));
        }
        return new MailSettings(mailProperties.getFrom(), null);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record MailSettings(String from, SmtpCredentials credentials) {
    }
}
