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
 * (KYC/medical data in this app's case).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void send(String from, String to, String subject, String htmlBody,
                      String attachmentFilename, byte[] attachmentBytes) {
        send(from, to, subject, htmlBody,
                attachmentBytes == null ? List.of() : List.of(new EmailAttachment(attachmentFilename, attachmentBytes)));
    }

    public void send(String from, String to, String subject, String htmlBody,
                      List<EmailAttachment> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
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
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email from [{}] to [{}] (subject=[{}]): {}",
                    from, to, subject, ex.getMessage(), ex);
        }
    }

    public void send(String from, String to, String subject, String htmlBody) {
        send(from, to, subject, htmlBody, List.of());
    }
}
