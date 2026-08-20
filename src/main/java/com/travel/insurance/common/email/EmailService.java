package com.travel.insurance.common.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thin wrapper over {@link JavaMailSender}, deliberately generic (no domain
 * knowledge) — mirrors {@code common/messaging/EventPublisher}'s catch-and-log
 * style so a broken mail server never propagates as an exception to a caller.
 * Never log the message body or attachment bytes here: callers pass PII
 * (KYC/medical data in this app's case). Callers may pass {@link SmtpCredentials}
 * to relay a single send through a different mail server (e.g. a per-tenant
 * mailbox) instead of the globally configured {@link #mailSender}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SmtpSenderFactory smtpSenderFactory;

    public void send(String from, String to, String subject, String htmlBody,
                      String attachmentFilename, byte[] attachmentBytes) {
        send(from, to, subject, htmlBody,
                attachmentBytes == null ? List.of() : List.of(new EmailAttachment(attachmentFilename, attachmentBytes)));
    }

    public void send(String from, String to, String subject, String htmlBody,
                      List<EmailAttachment> attachments) {
        sendVia(null, from, to, subject, htmlBody, attachments);
    }

    public void send(String from, String to, String subject, String htmlBody) {
        sendVia(null, from, to, subject, htmlBody, List.of());
    }

    public void send(SmtpCredentials credentials, String from, String to, String subject, String htmlBody,
                      List<EmailAttachment> attachments) {
        sendVia(credentials, from, to, subject, htmlBody, attachments);
    }

    public void send(SmtpCredentials credentials, String from, String to, String subject, String htmlBody) {
        sendVia(credentials, from, to, subject, htmlBody, List.of());
    }

    private void sendVia(SmtpCredentials credentials, String from, String to, String subject, String htmlBody,
                          List<EmailAttachment> attachments) {
        try {
            JavaMailSender sender = credentials != null ? smtpSenderFactory.create(credentials) : mailSender;
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (attachments != null) {
                for (EmailAttachment attachment : attachments) {
                    if (attachment != null && attachment.content() != null) {
                        helper.addAttachment(attachment.filename(), new ByteArrayResource(attachment.content()));
                    }
                }
            }
            sender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email from [{}] to [{}] (subject=[{}]): {}",
                    from, to, subject, ex.getMessage(), ex);
        }
    }
}
