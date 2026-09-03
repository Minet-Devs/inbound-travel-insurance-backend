package com.travel.insurance.notification;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.common.email.SmtpCredentials;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.organization.Organization;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.VisitorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelcomePackNotificationListenerTest {

    private static final UUID WELCOME_PACK_SENDER_ORGANIZATION_ID =
            UUID.fromString("db705c1e-05e8-48c6-b0ea-62237256e7b3");

    @Mock
    private VisitorService visitorService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EmailService emailService;

    private WelcomePackNotificationListener listener;

    private final UUID visitorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFrom("no-reply@travelinsurance.example");

        listener = new WelcomePackNotificationListener(visitorService, organizationService, emailService, mailProperties);
    }

    private Visitor sampleVisitor(VisitorStatus status) {
        Visitor visitor = new Visitor();
        visitor.setFullName("Jane Doe");
        visitor.setEmail("traveler@example.com");
        visitor.setVisitorStatus(status);
        return visitor;
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
    void sendsWelcomePackEmailUsingOrganizationMailboxWhenVisitorIsActive() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor(VisitorStatus.ACTIVE));
        when(organizationService.getEntityById(WELCOME_PACK_SENDER_ORGANIZATION_ID)).thenReturn(sampleOrganization());

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, UUID.randomUUID()));

        ArgumentCaptor<SmtpCredentials> credentialsCaptor = ArgumentCaptor.forClass(SmtpCredentials.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(credentialsCaptor.capture(), eq("no_reply@acme.example"),
                eq("traveler@example.com"), eq("RE: Welcome to Kenya – Your Medical Cover Is Now Active"),
                bodyCaptor.capture());

        assertThat(credentialsCaptor.getValue().host()).isEqualTo("smtp.acme.example");
        assertThat(bodyCaptor.getValue()).contains("Dear Jane,").contains("+254 719 044 777");
    }

    @Test
    void fallsBackToDefaultMailboxWhenOrganizationNotFullyConfigured() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor(VisitorStatus.ACTIVE));
        when(organizationService.getEntityById(WELCOME_PACK_SENDER_ORGANIZATION_ID)).thenReturn(new Organization());

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, UUID.randomUUID()));

        verify(emailService).send(eq((SmtpCredentials) null), eq("no-reply@travelinsurance.example"),
                eq("traveler@example.com"), anyString(), anyString());
    }

    @Test
    void fallsBackToDefaultMailboxWhenOrganizationNotFound() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor(VisitorStatus.ACTIVE));
        when(organizationService.getEntityById(WELCOME_PACK_SENDER_ORGANIZATION_ID))
                .thenThrow(new ResourceNotFoundException("Organization", WELCOME_PACK_SENDER_ORGANIZATION_ID));

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, UUID.randomUUID()));

        verify(emailService).send(eq((SmtpCredentials) null), eq("no-reply@travelinsurance.example"),
                eq("traveler@example.com"), anyString(), anyString());
    }

    @Test
    void doesNotSendEmailWhenVisitorIsNotActive() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor(VisitorStatus.PENDING));

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, UUID.randomUUID()));

        verifyNoInteractions(organizationService);
        verify(emailService, never()).send(eq((SmtpCredentials) null), anyString(), anyString(), anyString(), anyString());
    }
}
