package com.travel.insurance.common.email;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class DefaultSmtpSenderFactory implements SmtpSenderFactory {

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

        return sender;
    }
}
