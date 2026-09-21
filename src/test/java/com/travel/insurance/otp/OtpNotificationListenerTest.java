package com.travel.insurance.otp;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.common.email.SmtpCredentials;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.organization.Organization;
import com.travel.insurance.organization.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpNotificationListenerTest {

    private static final UUID OTP_SENDER_ORGANIZATION_ID = UUID.fromString("db705c1e-05e8-48c6-b0ea-62237256e7b3");

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EmailService emailService;

    private OtpNotificationListener listener;

    private final UUID otpId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFrom("no-reply@travelinsurance.example");

        listener = new OtpNotificationListener(otpRepository, organizationService, emailService, mailProperties);
    }

    private Otp sampleOtp() {
        Otp otp = new Otp();
        otp.setOtp("123456");
        otp.setEmail("traveler@example.com");
        otp.setExpiryTime(Instant.now().plusSeconds(600));
        otp.setServiceProviderId(UUID.randomUUID());
        return otp;
    }

    private Organization sampleOrganization() {
        Organization organization = new Organization();
        organization.setHost("smtp.acme.example");
        organization.setPort(587);
        organization.setNotificationEmail("no_reply@acme.example");
        organization.setNotificationEmailPassword("s3cr3t");
        return organization;
    }

    @Test
    void sendsOtpEmailUsingOrganizationMailboxWhenFullyConfigured() {
        when(otpRepository.findById(otpId)).thenReturn(java.util.Optional.of(sampleOtp()));
        when(organizationService.getEntityById(OTP_SENDER_ORGANIZATION_ID)).thenReturn(sampleOrganization());

        listener.onOtpGenerated(new OtpGeneratedEvent(otpId));

        ArgumentCaptor<SmtpCredentials> credentialsCaptor = ArgumentCaptor.forClass(SmtpCredentials.class);
        org.mockito.Mockito.verify(emailService).send(credentialsCaptor.capture(), eq("no_reply@acme.example"),
                eq("traveler@example.com"), eq("Your verification code"), org.mockito.ArgumentMatchers.anyString());

        assertThat(credentialsCaptor.getValue().host()).isEqualTo("smtp.acme.example");
        assertThat(credentialsCaptor.getValue().port()).isEqualTo(587);
        assertThat(credentialsCaptor.getValue().username()).isEqualTo("no_reply@acme.example");
        assertThat(credentialsCaptor.getValue().password()).isEqualTo("s3cr3t");
    }

    @Test
    void fallsBackToDefaultMailboxWhenOrganizationNotFullyConfigured() {
        when(otpRepository.findById(otpId)).thenReturn(java.util.Optional.of(sampleOtp()));
        when(organizationService.getEntityById(OTP_SENDER_ORGANIZATION_ID)).thenReturn(new Organization());

        listener.onOtpGenerated(new OtpGeneratedEvent(otpId));

        org.mockito.Mockito.verify(emailService).send(eq((SmtpCredentials) null),
                eq("no-reply@travelinsurance.example"), eq("traveler@example.com"),
                eq("Your verification code"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void fallsBackToDefaultMailboxWhenOrganizationNotFound() {
        when(otpRepository.findById(otpId)).thenReturn(java.util.Optional.of(sampleOtp()));
        when(organizationService.getEntityById(OTP_SENDER_ORGANIZATION_ID))
                .thenThrow(new ResourceNotFoundException("Organization", OTP_SENDER_ORGANIZATION_ID));

        listener.onOtpGenerated(new OtpGeneratedEvent(otpId));

        org.mockito.Mockito.verify(emailService).send(eq((SmtpCredentials) null),
                eq("no-reply@travelinsurance.example"), eq("traveler@example.com"),
                eq("Your verification code"), org.mockito.ArgumentMatchers.anyString());
    }
}
