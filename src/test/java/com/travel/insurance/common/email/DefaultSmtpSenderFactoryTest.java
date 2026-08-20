package com.travel.insurance.common.email;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSmtpSenderFactoryTest {

    @Test
    void buildsJavaMailSenderImplFromCredentials() {
        DefaultSmtpSenderFactory factory = new DefaultSmtpSenderFactory();
        SmtpCredentials credentials = new SmtpCredentials("smtp.acme.example", 587, "notify@acme.example", "s3cr3t");

        JavaMailSender sender = factory.create(credentials);

        assertThat(sender).isInstanceOfSatisfying(JavaMailSenderImpl.class, impl -> {
            assertThat(impl.getHost()).isEqualTo("smtp.acme.example");
            assertThat(impl.getPort()).isEqualTo(587);
            assertThat(impl.getUsername()).isEqualTo("notify@acme.example");
            assertThat(impl.getPassword()).isEqualTo("s3cr3t");
        });
    }
}
