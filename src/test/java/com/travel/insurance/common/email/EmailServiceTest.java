package com.travel.insurance.common.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SmtpSenderFactory smtpSenderFactory;

    private EmailService emailService;

    private MimeMessage newMimeMessage() {
        return new MimeMessage(Session.getDefaultInstance(new Properties()));
    }

    @Test
    void sendsMimeMessageWithAttachment() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.send("from@example.com", "to@example.com", "Subject",
                "<p>Body</p>", "certificate.pdf", "%PDF-1.4".getBytes());

        verify(mailSender).send(message);
    }

    @Test
    void sendsMimeMessageWithMultipleAttachments() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.send("from@example.com", "to@example.com", "Subject", "<p>Body</p>",
                List.of(new EmailAttachment("certificate.pdf", "%PDF-1".getBytes()),
                        new EmailAttachment("policy.pdf", "%PDF-2".getBytes())));

        verify(mailSender).send(message);
    }

    @Test
    void doesNotPropagateWhenSendFails() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.send("from@example.com", "to@example.com", "Subject",
                "<p>Body</p>", "certificate.pdf", "%PDF-1.4".getBytes()))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotPropagateWhenMimeMessageCreationFails() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> emailService.send("from@example.com", "to@example.com", "Subject",
                "<p>Body</p>", "certificate.pdf", "%PDF-1.4".getBytes()))
                .doesNotThrowAnyException();
    }

    private static MailSendException concurrentLimitFailure() {
        SMTPSendFailedException smtp = new SMTPSendFailedException("RCPT", 432,
                "4.3.2 Concurrent connections limit exceeded", null, null, null, null);
        return new MailSendException("Failed messages", smtp);
    }

    @Test
    void retriesTransientSmtpFailureAndReportsSuccess() {
        emailService = new EmailService(mailSender, smtpSenderFactory, 0);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(concurrentLimitFailure()).doNothing().when(mailSender).send(any(MimeMessage.class));

        boolean sent = emailService.send(null, "from@example.com", "to@example.com", List.of(), "Subject",
                "<p>Body</p>", List.of());

        assertThat(sent).isTrue();
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void givesUpAfterMaxAttemptsOnPersistentTransientFailure() {
        emailService = new EmailService(mailSender, smtpSenderFactory, 0);
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
        doThrow(concurrentLimitFailure()).when(mailSender).send(any(MimeMessage.class));

        boolean sent = emailService.send(null, "from@example.com", "to@example.com", List.of(), "Subject",
                "<p>Body</p>", List.of());

        assertThat(sent).isFalse();
        verify(mailSender, times(EmailService.MAX_ATTEMPTS)).send(any(MimeMessage.class));
    }

    @Test
    void doesNotRetryPermanentFailure() {
        emailService = new EmailService(mailSender, smtpSenderFactory, 0);
        when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        boolean sent = emailService.send(null, "from@example.com", "to@example.com", List.of(), "Subject",
                "<p>Body</p>", List.of());

        assertThat(sent).isFalse();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendsPlainTextEmail() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.send("from@example.com", "to@example.com", "Subject", "<p>Plain text body</p>");

        verify(mailSender).send(message);
    }

    @Test
    void doesNotPropagateWhenPlainTextSendFails() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.send("from@example.com", "to@example.com", "Subject", "<p>Body</p>"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendsViaCustomSmtpSenderWhenCredentialsProvided() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        JavaMailSender customSender = mock(JavaMailSender.class);
        MimeMessage message = newMimeMessage();
        SmtpCredentials credentials = new SmtpCredentials("smtp.acme.example", 587, "notify@acme.example", "s3cr3t");
        when(smtpSenderFactory.create(credentials)).thenReturn(customSender);
        when(customSender.createMimeMessage()).thenReturn(message);

        emailService.send(credentials, "notify@acme.example", "to@example.com", "Subject", "<p>Body</p>");

        verify(customSender).send(message);
        verifyNoInteractions(mailSender);
    }

    @Test
    void logsSmtpHostPortAndUsernameWhenCustomCredentialsProvided(CapturedOutput output) {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        JavaMailSender customSender = mock(JavaMailSender.class);
        MimeMessage message = newMimeMessage();
        SmtpCredentials credentials = new SmtpCredentials("smtp.acme.example", 587, "notify@acme.example", "s3cr3t");
        when(smtpSenderFactory.create(credentials)).thenReturn(customSender);
        when(customSender.createMimeMessage()).thenReturn(message);

        emailService.send(credentials, "notify@acme.example", "to@example.com", "Subject", "<p>Body</p>");

        assertThat(output.getOut())
                .contains("smtp.acme.example")
                .contains("587")
                .contains("notify@acme.example")
                .doesNotContain("s3cr3t");
    }

    @Test
    void fallsBackToDefaultSenderWhenCredentialsNull() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.send(null, "from@example.com", "to@example.com", "Subject", "<p>Body</p>");

        verify(mailSender).send(message);
        verifyNoInteractions(smtpSenderFactory);
    }

    @Test
    void doesNotPropagateWhenSmtpSenderFactoryThrows() {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        SmtpCredentials credentials = new SmtpCredentials("smtp.acme.example", 587, "notify@acme.example", "s3cr3t");
        when(smtpSenderFactory.create(credentials)).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> emailService.send(credentials, "notify@acme.example", "to@example.com", "Subject",
                "<p>Body</p>"))
                .doesNotThrowAnyException();
    }

    @Test
    void addsBccRecipientsWithoutExposingThemInToHeader() throws Exception {
        emailService = new EmailService(mailSender, smtpSenderFactory);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.send(null, "from@example.com", "to@example.com",
                List.of("a@example.com", "b@example.com"), "Subject", "<p>Body</p>", List.of());

        verify(mailSender).send(message);
        assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.BCC))
                .extracting(Object::toString).containsExactly("a@example.com", "b@example.com");
        assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.TO))
                .extracting(Object::toString).containsExactly("to@example.com");
    }
}
