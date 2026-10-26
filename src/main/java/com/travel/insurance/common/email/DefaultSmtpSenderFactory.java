package com.travel.insurance.common.email;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class DefaultSmtpSenderFactory implements SmtpSenderFactory {

    /** Without these JavaMail waits forever, so a hung relay would pin the sender indefinitely. */
    private static final String SMTP_TIMEOUT_MILLIS = "10000";

    @Override
    public JavaMailSender create(SmtpCredentials credentials) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(credentials.host());
        sender.setPort(credentials.port());
        sender.setUsername(credentials.username());
        sender.setPassword(credentials.password());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", SMTP_TIMEOUT_MILLIS);
        props.put("mail.smtp.timeout", SMTP_TIMEOUT_MILLIS);
        props.put("mail.smtp.writetimeout", SMTP_TIMEOUT_MILLIS);

        return sender;
    }
}
