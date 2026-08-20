package com.travel.insurance.common.email;

/**
 * Per-send SMTP override. When passed to {@link EmailService}, the email is
 * relayed through a {@code JavaMailSender} built from these credentials
 * instead of the globally configured one.
 */
public record SmtpCredentials(String host, int port, String username, String password) {
}
