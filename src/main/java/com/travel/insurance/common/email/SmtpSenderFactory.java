package com.travel.insurance.common.email;

import org.springframework.mail.javamail.JavaMailSender;

public interface SmtpSenderFactory {

    JavaMailSender create(SmtpCredentials credentials);
}
