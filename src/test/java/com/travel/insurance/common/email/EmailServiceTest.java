package com.travel.insurance.common.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    private MimeMessage newMimeMessage() {
        return new MimeMessage(Session.getDefaultInstance(new Properties()));
    }

    @Test
    void sendsMimeMessageWithAttachment() {
        emailService = new EmailService(mailSender);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.send("from@example.com", "to@example.com", "Subject",
                "<p>Body</p>", "certificate.pdf", "%PDF-1.4".getBytes());

        verify(mailSender).send(message);
    }

    @Test
    void doesNotPropagateWhenSendFails() {
        emailService = new EmailService(mailSender);
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.send("from@example.com", "to@example.com", "Subject",
                "<p>Body</p>", "certificate.pdf", "%PDF-1.4".getBytes()))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotPropagateWhenMimeMessageCreationFails() {
        emailService = new EmailService(mailSender);
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> emailService.send("from@example.com", "to@example.com", "Subject",
                "<p>Body</p>", "certificate.pdf", "%PDF-1.4".getBytes()))
                .doesNotThrowAnyException();
    }
}
