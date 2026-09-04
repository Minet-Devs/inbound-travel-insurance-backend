package com.travel.insurance.notification;

import com.travel.insurance.common.email.EmailAttachment;
import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.common.email.SmtpCredentials;
import com.travel.insurance.common.util.LogoUrlNormalizer;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.notification.PolicyDocumentData.BenefitLine;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.premiumreceipt.PremiumReceiptService;
import com.travel.insurance.premiumreceipt.dto.PremiumReceiptResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.VisitorStatus;
import com.travel.insurance.visitor.VisitorStatusChangedEvent;
import com.travel.insurance.visitorbenefit.VisitorBenefitService;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emails the visitor their personalized policy certificate and Welcome Pack
 * once their cover is active — either on a status transition to ACTIVE, or on
 * creation when the visitor is created already ACTIVE (the default). Both
 * paths are gated on the ACTIVE status so exactly one email goes out per
 * activation. This is the visitor's only activation email — it used to be
 * split into a separate "Welcome Pack" notification, but that was folded in
 * here so the visitor gets one email, not two (per {@code prompts.md}).
 * Unlike {@code visitorbenefit.VisitorStatusChangedListener}
 * (which stays synchronous and in-transaction because it must mirror the
 * status onto VisitorBenefit rows consistently), this listener uses
 * {@code AFTER_COMMIT}: sending mail over SMTP inside the same transaction
 * that changed the visitor's status would risk rolling back a legitimate
 * status change if the mail server is slow or unreachable. Any failure here
 * is caught and logged, never propagated — a broken mail server must never
 * be able to affect the visitor status API's correctness. Re-activation
 * (e.g. ACTIVE → SUSPENDED → ACTIVE) intentionally re-sends the email;
 * that's treated as a new, valid activation rather than a duplicate to guard
 * against.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VisitorActivatedNotificationListener {

    private static final String POLICY_DOCUMENT_RESOURCE = "templates/Policy_Document_July_2026.pdf";
    private static final String POLICY_DOCUMENT_ATTACHMENT_NAME = "Policy_Document_July_2026.pdf";
    private static final String WELCOME_PACK_RESOURCE = "templates/Inbound-Travel-Health-Welcome-Pack.pdf";
    private static final String WELCOME_PACK_ATTACHMENT_NAME = "Inbound-Travel-Health-Welcome-Pack.pdf";

    private byte[] rawPolicyDocumentCache;
    private byte[] welcomePackPdfCache;
    private final Map<UUID, byte[]> brandedPolicyDocumentCache = new ConcurrentHashMap<>();

    private final VisitorService visitorService;
    private final PolicyService policyService;
    private final VisitorBenefitService visitorBenefitService;
    private final InsurerService insurerService;
    private final PremiumReceiptService premiumReceiptService;
    private final PolicyDocumentRenderer renderer;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVisitorStatusChanged(VisitorStatusChangedEvent event) {
        if (event.newStatus() != VisitorStatus.ACTIVE) {
            return;
        }
        sendActivationDocumentQuietly(event.visitorId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVisitorCreated(VisitorCreatedEvent event) {
        if (visitorService.getEntityById(event.visitorId()).getVisitorStatus() != VisitorStatus.ACTIVE) {
            return;
        }
        sendActivationDocumentQuietly(event.visitorId());
    }

    private void sendActivationDocumentQuietly(UUID visitorId) {
        try {
            sendActivationDocument(visitorId);
        } catch (Exception ex) {
            log.error("Failed to generate/send activation email for visitor {}: {}",
                    visitorId, ex.getMessage(), ex);
        }
    }

    private void sendActivationDocument(UUID visitorId) {
        Visitor visitor = visitorService.getEntityById(visitorId);
        Policy policy = policyService.getEntityById(visitor.getPolicyId());
        List<VisitorBenefitResponse> visitorBenefits = visitorBenefitService.listAllByVisitor(visitorId);
        if (visitorBenefits.isEmpty()) {
            log.warn("Visitor {} activated with no assigned benefits yet; sending certificate without a schedule",
                    visitorId);
        }

        Insurer insurer = insurerService.getEntityById(policy.getInsurerId());
        List<String> insurerNames = List.of(insurer.getName());
        String underwriterLogoUrl = normalizeOrNull(insurer.getLogoUrl());
        String esignatureUrl = normalizeOrNull(insurer.getEsignature());
        List<BenefitLine> benefitLines = visitorBenefits.stream()
                .map(vb -> new BenefitLine(vb.benefitName(), vb.limitAmount()))
                .toList();

        PolicyDocumentData data = new PolicyDocumentData(
                visitor.getFullName(),
                visitor.getPassportNumber(),
                visitor.getCertificateSerialNumber(),
                visitor.getDateOfBirth(),
                visitor.getGender(),
                visitor.getNationality(),
                visitor.getAddress(),
                visitor.getEmail(),
                visitor.getPhoneNumber(),
                visitor.getDateIn(),
                visitor.getDateOut(),
                visitor.getPolicyExpiryDate(),
                visitor.getReasonForTravel(),
                insurerNames,
                underwriterLogoUrl,
                esignatureUrl,
                benefitLines,
                mailProperties.getEmergencyAssistance().getPhone(),
                mailProperties.getEmergencyAssistance().getEmail());

        byte[] pdf = renderer.renderPdf(data);

        PremiumReceiptResponse premiumReceipt = premiumReceiptService.get();
        PremiumReceiptData premiumReceiptData = new PremiumReceiptData(
                visitor.getFullName(),
                visitor.getPassportNumber(),
                visitor.getCertificateSerialNumber(),
                visitor.getAddress(),
                visitor.getNationality(),
                insurer.getName(),
                underwriterLogoUrl,
                insurer.getAddress(),
                premiumReceipt.totalPremium());
        byte[] premiumReceiptPdf = renderer.renderPremiumReceiptPdf(premiumReceiptData);

        byte[] combinedPdf = renderer.mergePdfs(pdf, premiumReceiptPdf);
        List<EmailAttachment> attachments = new ArrayList<>();
        attachments.add(new EmailAttachment(
                "policy-certificate-" + visitor.getPassportNumber() + ".pdf", combinedPdf));

        byte[] policyDocument = loadBrandedPolicyDocument(insurer.getId(), underwriterLogoUrl, esignatureUrl);
        if (policyDocument != null) {
            attachments.add(new EmailAttachment(POLICY_DOCUMENT_ATTACHMENT_NAME, policyDocument));
        }
        byte[] welcomePackPdf = loadWelcomePackPdf();
        if (welcomePackPdf != null) {
            attachments.add(new EmailAttachment(WELCOME_PACK_ATTACHMENT_NAME, welcomePackPdf));
        }
        InsurerMailSettings mailSettings = resolveMailSettings(insurer);
        emailService.send(
                mailSettings.credentials(),
                mailSettings.from(),
                visitor.getEmail(),
                "Welcome to Kenya – Your Medical Cover Is Now Active",
                buildActivationEmailHtml(firstNameOf(visitor.getFullName())),
                attachments);
        log.info("Sent activation email for visitor {} to {}", visitorId, visitor.getEmail());
    }

    private static String firstNameOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Traveller";
        }
        return fullName.trim().split("\\s+")[0];
    }

    private static final String EMAIL_FONT_FAMILY = "Corbel, 'Segoe UI', Arial, sans-serif";

    private static String buildActivationEmailHtml(String firstName) {
        return "<div style=\"font-family: " + EMAIL_FONT_FAMILY + ";\">"
                + "<p>Dear " + firstName + ",</p>"
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
                + "Email: inbound.travel@minet.co.ke</p>"
                + "</div>";
    }

    /**
     * Loads the bundled Welcome Pack PDF from the classpath, cached after the
     * first read. A load failure is logged and returns {@code null} so the
     * email still goes out without the attachment.
     */
    private synchronized byte[] loadWelcomePackPdf() {
        if (welcomePackPdfCache == null) {
            try {
                welcomePackPdfCache = new ClassPathResource(WELCOME_PACK_RESOURCE).getInputStream().readAllBytes();
            } catch (IOException ex) {
                log.error("Could not load bundled welcome pack {}: {}", WELCOME_PACK_RESOURCE, ex.getMessage(), ex);
                return null;
            }
        }
        return welcomePackPdfCache;
    }

    /**
     * An insurer's own mailbox is used only when it has fully configured its
     * SMTP relay (host, port, notification email and password all set) — a
     * custom {@code from} address is never mixed with the global relay, since
     * most SMTP servers reject relaying on behalf of an unrelated sender
     * (SPF/DKIM). Otherwise every field falls back to the global
     * {@link MailProperties}, exactly as before this feature existed.
     */
    private InsurerMailSettings resolveMailSettings(Insurer insurer) {
        boolean fullyConfigured = isNotBlank(insurer.getHost())
                && insurer.getPort() != null
                && isNotBlank(insurer.getNotificationEmail())
                && isNotBlank(insurer.getNotificationEmailPassword());
        if (fullyConfigured) {
            return new InsurerMailSettings(
                    insurer.getNotificationEmail(),
                    new SmtpCredentials(insurer.getHost(), insurer.getPort(),
                            insurer.getNotificationEmail(), insurer.getNotificationEmailPassword()));
        }
        return new InsurerMailSettings(mailProperties.getFrom(), null);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeOrNull(String url) {
        return isNotBlank(url) ? LogoUrlNormalizer.normalize(url) : null;
    }

    private record InsurerMailSettings(String from, SmtpCredentials credentials) {
    }

    /**
     * Loads the bundled policy wording PDF from the classpath, cached after the
     * first read. A load failure is logged and returns {@code null} so the
     * certificate still goes out without the supplementary document.
     */
    private synchronized byte[] loadRawPolicyDocument() {
        if (rawPolicyDocumentCache == null) {
            try {
                rawPolicyDocumentCache = new ClassPathResource(POLICY_DOCUMENT_RESOURCE)
                        .getInputStream().readAllBytes();
            } catch (IOException ex) {
                log.error("Could not load bundled policy document {}: {}",
                        POLICY_DOCUMENT_RESOURCE, ex.getMessage(), ex);
                return null;
            }
        }
        return rawPolicyDocumentCache;
    }

    /**
     * Returns the policy wording PDF branded with the insurer's logo (page 1)
     * and e-signature (last page), cached per insurer for the process
     * lifetime — same tradeoff as the previous single cached copy, now keyed
     * by insurer since the content differs per insurer. A branding failure
     * (e.g. the logo/e-signature URL is unreachable) falls back to the
     * unbranded document rather than dropping the attachment entirely.
     */
    private byte[] loadBrandedPolicyDocument(UUID insurerId, String logoUrl, String esignatureUrl) {
        byte[] raw = loadRawPolicyDocument();
        if (raw == null) {
            return null;
        }
        if (logoUrl == null && esignatureUrl == null) {
            return raw;
        }
        return brandedPolicyDocumentCache.computeIfAbsent(insurerId, id -> {
            try {
                return renderer.brandPolicyWording(raw, logoUrl, esignatureUrl);
            } catch (Exception ex) {
                log.error("Failed to brand policy document for insurer {}: {}", id, ex.getMessage(), ex);
                return raw;
            }
        });
    }
}
