package com.travel.insurance.common.email;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thin wrapper over {@link JavaMailSender}, deliberately generic (no domain
 * knowledge) — mirrors {@code common/messaging/EventPublisher}'s catch-and-log
 * style so a broken mail server never propagates as an exception to a caller.
 * Never log the message body or attachment bytes here: callers pass PII
 * (KYC/medical data in this app's case). Callers may pass {@link SmtpCredentials}
 * to relay a single send through a different mail server (e.g. a per-tenant
 * mailbox) instead of the globally configured {@link #mailSender}.
 * <p>
 * Sends are serialized and transient SMTP failures (4xx replies, e.g. Office 365's
 * "432 Concurrent connections limit exceeded") are retried with backoff. Callers that
 * must know whether the mail went out can use the {@code boolean}-returning overload.
 */
@Component
@Slf4j
public class EmailService {

    static final int MAX_ATTEMPTS = 3;
    private static final long DEFAULT_BACKOFF_MILLIS = 2_000;

    private final JavaMailSender mailSender;
    private final SmtpSenderFactory smtpSenderFactory;
    private final long backoffMillis;
    private final ReentrantLock sendLock = new ReentrantLock();

    @Autowired
    public EmailService(JavaMailSender mailSender, SmtpSenderFactory smtpSenderFactory) {
        this(mailSender, smtpSenderFactory, DEFAULT_BACKOFF_MILLIS);
    }

    EmailService(JavaMailSender mailSender, SmtpSenderFactory smtpSenderFactory, long backoffMillis) {
        this.mailSender = mailSender;
        this.smtpSenderFactory = smtpSenderFactory;
        this.backoffMillis = backoffMillis;
    }

    public void send(String from, String to, String subject, String htmlBody,
                      String attachmentFilename, byte[] attachmentBytes) {
        send(from, to, subject, htmlBody,
                attachmentBytes == null ? List.of() : List.of(new EmailAttachment(attachmentFilename, attachmentBytes)));
    }

    public void send(String from, String to, String subject, String htmlBody,
                      List<EmailAttachment> attachments) {
        sendVia(null, from, to, List.of(), subject, htmlBody, attachments);
    }

    public void send(String from, String to, String subject, String htmlBody) {
        sendVia(null, from, to, List.of(), subject, htmlBody, List.of());
    }

    public void send(SmtpCredentials credentials, String from, String to, String subject, String htmlBody,
                      List<EmailAttachment> attachments) {
        sendVia(credentials, from, to, List.of(), subject, htmlBody, attachments);
    }

    /** @return {@code true} if the mail was accepted by the SMTP server, {@code false} if all attempts failed */
    public boolean send(SmtpCredentials credentials, String from, String to, List<String> bcc, String subject,
                         String htmlBody, List<EmailAttachment> attachments) {
        return sendVia(credentials, from, to, bcc, subject, htmlBody, attachments);
    }

    public void send(SmtpCredentials credentials, String from, String to, String subject, String htmlBody) {
        sendVia(credentials, from, to, List.of(), subject, htmlBody, List.of());
    }

    private boolean sendVia(SmtpCredentials credentials, String from, String to, List<String> bcc,
                             String subject, String htmlBody, List<EmailAttachment> attachments) {
        sendLock.lock();
        try {
            for (int attempt = 1; ; attempt++) {
                try {
                    doSend(credentials, from, to, bcc, subject, htmlBody, attachments);
                    return true;
                } catch (Exception ex) {
                    if (attempt < MAX_ATTEMPTS && isTransient(ex)) {
                        log.warn("Transient failure sending email to [{}] (attempt {}/{}), retrying: {}",
                                to, attempt, MAX_ATTEMPTS, ex.getMessage());
                        if (!pause(backoffMillis * attempt)) {
                            return false;
                        }
                        continue;
                    }
                    log.error("Failed to send email from [{}] to [{}] (subject=[{}]): {}",
                            from, to, subject, ex.getMessage(), ex);
                    return false;
                }
            }
        } finally {
            sendLock.unlock();
        }
    }

    private void doSend(SmtpCredentials credentials, String from, String to, List<String> bcc,
                         String subject, String htmlBody, List<EmailAttachment> attachments) throws Exception {
        if (credentials != null) {
            log.info("Sending email via SMTP host [{}:{}] as user [{}]",
                    credentials.host(), credentials.port(), credentials.username());
        }
        JavaMailSender sender = credentials != null ? smtpSenderFactory.create(credentials) : mailSender;
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        if (bcc != null && !bcc.isEmpty()) {
            helper.setBcc(bcc.toArray(String[]::new));
        }
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
    }

    private static boolean isTransient(Throwable ex) {
        if (ex instanceof MailSendException mse) {
            for (Exception failed : mse.getFailedMessages().values()) {
                if (isTransient(failed)) {
                    return true;
                }
            }
        }
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SMTPSendFailedException smtp) {
                return smtp.getReturnCode() >= 400 && smtp.getReturnCode() < 500;
            }
        }
        return false;
    }

    private static boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
