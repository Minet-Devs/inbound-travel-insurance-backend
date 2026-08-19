package com.travel.insurance.ussd.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class FeedbackEmailService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String recipientAddress;

    public FeedbackEmailService(JavaMailSender mailSender,
                                @Value("${ussd.feedback.email.from:ussd-feedback@travelinsurance.example}") String fromAddress,
                                @Value("${ussd.feedback.email.to:feedback-recipient@travelinsurance.example}") String recipientAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.recipientAddress = recipientAddress;
    }

    public void sendFeedbackEmail(String senderMsisdn, String message) {
        String subject = "USSD Feedback";

        String body = "Feedback received via USSD:\n"
                + "\n"
                + "MSISDN: " + (senderMsisdn != null ? senderMsisdn : "N/A") + "\n"
                + "Message: " + message + "\n"
                + "Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipientAddress);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(mimeMessage);
            log.info("USSD feedback email sent to {}", recipientAddress);
        } catch (Exception e) {
            log.error("Failed to send USSD feedback email to {}: {}", recipientAddress, e.getMessage(), e);
        }
    }

}
