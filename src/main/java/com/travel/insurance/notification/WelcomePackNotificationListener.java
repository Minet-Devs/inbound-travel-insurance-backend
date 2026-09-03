package com.travel.insurance.notification;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.common.email.SmtpCredentials;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.organization.Organization;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.VisitorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Sends the Welcome Pack email — a second, separate notification from the
 * policy document email sent by {@link VisitorActivatedNotificationListener}
 * — once a visitor's cover is active. Runs AFTER_COMMIT for the same reason
 * as the other visitor notification listeners: an SMTP call must never be
 * able to roll back the transaction that created the visitor. The mailbox
 * is resolved from a fixed organization id, mirroring
 * {@code otp.OtpNotificationListener}, per the current spec.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WelcomePackNotificationListener {

    private static final UUID WELCOME_PACK_SENDER_ORGANIZATION_ID =
            UUID.fromString("db705c1e-05e8-48c6-b0ea-62237256e7b3");

    private final VisitorService visitorService;
    private final OrganizationService organizationService;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVisitorCreated(VisitorCreatedEvent event) {
        try {
            sendWelcomePackEmail(event.visitorId());
        } catch (Exception ex) {
            log.error("Failed to send welcome pack email for visitor {}: {}",
                    event.visitorId(), ex.getMessage(), ex);
        }
    }

    private void sendWelcomePackEmail(UUID visitorId) {
        Visitor visitor = visitorService.getEntityById(visitorId);
        if (visitor.getVisitorStatus() != VisitorStatus.ACTIVE) {
            return;
        }

        MailSettings mailSettings = resolveMailSettings();
        emailService.send(
                mailSettings.credentials(),
                mailSettings.from(),
                visitor.getEmail(),
                "RE: Welcome to Kenya – Your Medical Cover Is Now Active",
                buildWelcomePackHtml(firstNameOf(visitor.getFullName())));
        log.info("Sent welcome pack email for visitor {} to {}", visitorId, visitor.getEmail());
    }

    private static String firstNameOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Traveller";
        }
        return fullName.trim().split("\\s+")[0];
    }

    private static String buildWelcomePackHtml(String firstName) {
        return "<p>Dear " + firstName + ",</p>"
                + "<p>Welcome to Kenya!</p>"
                + "<p>We are pleased to confirm that your Inbound Travel Health Insurance cover has been "
                + "activated following your arrival in the country. Your cover will remain valid in "
                + "accordance with the period stated in your policy document.</p>"
                + "<p>To help you access assistance and medical care easily during your stay, we have "
                + "attached your Welcome Pack. It contains:</p>"
                + "<ul>"
                + "<li>Your 24-hour emergency and assistance contacts</li>"
                + "<li>Your medical cover benefits</li>"
                + "<li>Instructions for accessing medical care</li>"
                + "<li>Details on how to locate accredited hospitals across Kenya</li>"
                + "<li>Guidance on emergency evacuation and hospital admission</li>"
                + "<li>Information on downloading and using the mobile app</li>"
                + "</ul>"
                + "<p><strong>Download the mobile app:</strong><br>"
                + "Android: [Insert Google Play link]<br>"
                + "iPhone: [Insert Apple App Store link]</p>"
                + "<p><strong>How to find an accredited hospital:</strong></p>"
                + "<ul>"
                + "<li>Use the app and select “Find a Hospital”</li>"
                + "<li>If you have a Kenyan mobile line, dial *202*15#, select “Find Hospital,” "
                + "and follow the prompts</li>"
                + "<li>Call our 24/7 Assistance Centre on +254 719 044 777 for guidance to the nearest "
                + "appropriate facility</li>"
                + "</ul>"
                + "<p>If you require medical attention, please visit an accredited healthcare provider and "
                + "present:</p>"
                + "<ul>"
                + "<li>Your passport and</li>"
                + "<li>Your electronic insurance policy or confirmation of cover.</li>"
                + "</ul>"
                + "<p>Treatment at accredited facilities is provided on a cashless basis, subject to your "
                + "policy terms, benefit limits and the applicable authorization process.</p>"
                + "<p>In an emergency, please call +254 719 044 777 immediately, preferably before "
                + "incurring any medical expenses. If you are admitted to a non-accredited hospital because "
                + "of an emergency, you or someone assisting you must notify us within 24 hours.</p>"
                + "<p>For general enquiries, contact us at inbound.travel@minet.co.ke.</p>"
                + "<p>Please save the emergency number in your phone and keep the attached Welcome Pack "
                + "readily accessible throughout your stay.</p>"
                + "<p>We wish you a safe, healthy and enjoyable stay in Kenya.</p>"
                + "<p>Warm regards,</p>"
                + "<p>Inbound Travel Health Insurance Support Team<br>"
                + "Minet Kenya<br>"
                + "24/7 Assistance Centre: +254 719 044 777<br>"
                + "Email: inbound.travel@minet.co.ke</p>";
    }

    private MailSettings resolveMailSettings() {
        Organization organization;
        try {
            organization = organizationService.getEntityById(WELCOME_PACK_SENDER_ORGANIZATION_ID);
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
