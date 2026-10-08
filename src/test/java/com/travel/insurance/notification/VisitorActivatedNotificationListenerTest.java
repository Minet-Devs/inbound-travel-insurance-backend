package com.travel.insurance.notification;

import com.travel.insurance.common.email.EmailAttachment;
import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.common.email.SmtpCredentials;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.premiumreceipt.PremiumReceiptService;
import com.travel.insurance.visitor.Gender;
import com.travel.insurance.visitor.MaritalStatus;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.VisitorStatus;
import com.travel.insurance.visitor.VisitorStatusChangedEvent;
import com.travel.insurance.visitorbenefit.VisitorBenefitAssignedEvent;
import com.travel.insurance.visitorbenefit.VisitorBenefitService;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorActivatedNotificationListenerTest {

    @Mock
    private VisitorService visitorService;

    @Mock
    private PolicyService policyService;

    @Mock
    private VisitorBenefitService visitorBenefitService;

    @Mock
    private BenefitService benefitService;

    @Mock
    private InsurerService insurerService;

    @Mock
    private PremiumReceiptService premiumReceiptService;

    @Mock
    private PolicyDocumentRenderer renderer;

    @Mock
    private EmailService emailService;

    private VisitorActivatedNotificationListener listener;

    private final UUID visitorId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID insurerId = UUID.randomUUID();
    private final UUID catalogBenefitId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFrom("no-reply@travelinsurance.example");
        mailProperties.setActivationBcc(List.of("hussein.mishobo@minet.co.ke", "Linus.Kemboi@minet.co.ke"));
        mailProperties.getEmergencyAssistance().setPhone("+254 700 000000");
        mailProperties.getEmergencyAssistance().setEmail("assistance@example.com");

        listener = new VisitorActivatedNotificationListener(
                visitorService, policyService, visitorBenefitService, benefitService, insurerService,
                premiumReceiptService, renderer, emailService, mailProperties);
        lenient().when(emailService.send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), anyList()))
                .thenReturn(true);
        lenient().when(benefitService.listAll()).thenReturn(List.of(
                new BenefitResponse(catalogBenefitId, "Medical Expenses", new BigDecimal("20000.00"),
                        Instant.now(), Instant.now())));
    }

    private Visitor sampleVisitor() {
        Visitor visitor = new Visitor();
        visitor.setId(visitorId);
        visitor.setPolicyId(policyId);
        visitor.setFullName("Jane Traveler");
        visitor.setPassportNumber("P1234567");
        visitor.setCertificateSerialNumber("ACME-2026-000123");
        visitor.setDateOfBirth(LocalDate.of(1990, 5, 12));
        visitor.setGender(Gender.FEMALE);
        visitor.setNationality("Germany");
        visitor.setAddress("12 Example Street, Berlin");
        visitor.setEmail("jane.traveler@example.com");
        visitor.setPhoneNumber("+254700000000");
        visitor.setDateIn(LocalDate.of(2026, 8, 1));
        visitor.setDateOut(LocalDate.of(2026, 11, 1));
        visitor.setMaritalStatus(MaritalStatus.SINGLE);
        visitor.setReasonForTravel("Tourism");
        visitor.setFacePhotoUrl("https://storage.example.com/photos/jane.jpg");
        return visitor;
    }

    private Policy samplePolicy() {
        Policy policy = new Policy();
        policy.setInsurerId(insurerId);
        return policy;
    }

    private Insurer sampleInsurer() {
        Insurer insurer = new Insurer();
        insurer.setId(insurerId);
        insurer.setName("Acme Insurance");
        insurer.setContactEmail("contact@acme.example");
        insurer.setAddress("PO Box 500, Nairobi");
        return insurer;
    }

    private List<VisitorBenefitResponse> sampleBenefits() {
        return List.of(new VisitorBenefitResponse(UUID.randomUUID(), visitorId, catalogBenefitId,
                "Medical Expenses", new BigDecimal("20000.00"), BigDecimal.ZERO,
                new BigDecimal("20000.00"),
                VisitorStatus.ACTIVE, Instant.now(), Instant.now()));
    }

    @Test
    void ignoresTransitionsToNonActiveStatus() {
        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.SUSPENDED));

        verifyNoInteractions(visitorService, policyService, visitorBenefitService, insurerService,
                renderer, emailService);
    }

    @Test
    void sendsCertificateEmailWhenVisitorBecomesActive() {
        Insurer insurer = sampleInsurer();
        insurer.setLogoUrl("https://cdn.example/acme.png");
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(List.of(
                new VisitorBenefitResponse(UUID.randomUUID(), visitorId, catalogBenefitId,
                        "Medical Expenses", new BigDecimal("20000.00"), BigDecimal.ZERO,
                        new BigDecimal("20000.00"),
                        VisitorStatus.ACTIVE, Instant.now(), Instant.now())));
        when(insurerService.getEntityById(insurerId)).thenReturn(insurer);
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());
        when(renderer.mergePdfs("%PDF-1.4".getBytes(), "%PDF-RECEIPT".getBytes())).thenReturn("%PDF-MERGED".getBytes());
        when(renderer.brandPolicyWording(any(byte[].class), eq("https://cdn.example/acme.png"), isNull()))
                .thenReturn("%PDF-BRANDED".getBytes());
        when(renderer.fillPolicyAgreementDetails(eq("%PDF-BRANDED".getBytes()), anyString(), any(), anyString(),
                anyString(), any(LocalDate.class)))
                .thenReturn("%PDF-AGREEMENT-FILLED".getBytes());

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        ArgumentCaptor<PolicyDocumentData> dataCaptor = ArgumentCaptor.forClass(PolicyDocumentData.class);
        verify(visitorService).markActivationEmailSent(visitorId);
        verify(renderer).renderPdf(dataCaptor.capture());
        assertThat(dataCaptor.getValue().visitorFullName()).isEqualTo("Jane Traveler");
        assertThat(dataCaptor.getValue().certificateSerialNumber()).isEqualTo("ACME-2026-000123");
        assertThat(dataCaptor.getValue().insurerNames()).containsExactly("Acme Insurance");
        assertThat(dataCaptor.getValue().underwriterLogoUrl()).isEqualTo("https://cdn.example/acme.png");
        assertThat(dataCaptor.getValue().esignatureUrl()).isNull();
        assertThat(dataCaptor.getValue().benefits()).hasSize(1);

        verify(renderer).fillPolicyAgreementDetails("%PDF-BRANDED".getBytes(), "Acme Insurance",
                "PO Box 500, Nairobi", "Jane Traveler", "jane.traveler@example.com", LocalDate.now());

        ArgumentCaptor<PremiumReceiptData> receiptCaptor = ArgumentCaptor.forClass(PremiumReceiptData.class);
        verify(renderer).renderPremiumReceiptPdf(receiptCaptor.capture());
        assertThat(receiptCaptor.getValue().visitorFullName()).isEqualTo("Jane Traveler");
        assertThat(receiptCaptor.getValue().passportNumber()).isEqualTo("P1234567");
        assertThat(receiptCaptor.getValue().certificateSerialNumber()).isEqualTo("ACME-2026-000123");
        assertThat(receiptCaptor.getValue().visitorAddress()).isEqualTo("12 Example Street, Berlin");
        assertThat(receiptCaptor.getValue().visitorNationality()).isEqualTo("Germany");
        assertThat(receiptCaptor.getValue().insurerName()).isEqualTo("Acme Insurance");
        assertThat(receiptCaptor.getValue().insurerLogoUrl()).isEqualTo("https://cdn.example/acme.png");
        assertThat(receiptCaptor.getValue().insurerAddress()).isEqualTo("PO Box 500, Nairobi");
        assertThat(receiptCaptor.getValue().totalPremium()).isEqualTo(new BigDecimal("44"));
        verify(premiumReceiptService).calculateTotalPremium(36);

        ArgumentCaptor<List<String>> bccCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<EmailAttachment>> attachmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService).send(
                isNull(),
                eq("no-reply@travelinsurance.example"),
                eq("jane.traveler@example.com"),
                bccCaptor.capture(),
                subjectCaptor.capture(),
                bodyCaptor.capture(),
                attachmentsCaptor.capture());
        assertThat(bccCaptor.getValue()).containsExactly(
                "hussein.mishobo@minet.co.ke", "Linus.Kemboi@minet.co.ke");
        assertThat(subjectCaptor.getValue()).isEqualTo("Welcome to Kenya – Your Medical Cover Is Now Active");
        assertThat(bodyCaptor.getValue()).contains("Dear Jane,").contains("+254 719 044 777");
        assertThat(bodyCaptor.getValue())
                .contains("<a href=\"https://play.google.com/store/apps/details?id=com.kenyacares.mobile\">")
                .contains("<img src=\"https://dl.dropboxusercontent.com/scl/fi/5vd3sslx6kqno56l2ipty/playstorelogo.png")
                .contains("alt=\"Get it on Google Play\"")
                .doesNotContain("iPhone")
                .doesNotContain("App Store")
                .doesNotContain("[Insert");
        assertThat(attachmentsCaptor.getValue())
                .extracting(EmailAttachment::filename)
                .containsExactly("Insurance Policy.pdf", "Policy Document.pdf",
                        "Inbound-Travel-Health-Welcome-Pack.pdf");
        assertThat(attachmentsCaptor.getValue().get(0).content()).isEqualTo("%PDF-MERGED".getBytes());
        assertThat(attachmentsCaptor.getValue().get(1).content()).isEqualTo("%PDF-AGREEMENT-FILLED".getBytes());
    }

    @Test
    void normalizesLegacyDropboxLogoUrlBeforeRendering() {
        Insurer insurer = sampleInsurer();
        insurer.setLogoUrl("https://www.dropbox.com/scl/fi/abc/ga-logo.png?rlkey=key&dl=0");
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(insurer);
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        ArgumentCaptor<PolicyDocumentData> dataCaptor = ArgumentCaptor.forClass(PolicyDocumentData.class);
        verify(renderer).renderPdf(dataCaptor.capture());
        assertThat(dataCaptor.getValue().underwriterLogoUrl())
                .isEqualTo("https://dl.dropboxusercontent.com/scl/fi/abc/ga-logo.png?rlkey=key&dl=1");
    }

    @Test
    void doesNotPropagateWhenRendererThrows() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(sampleInsurer());
        when(renderer.renderPdf(any(PolicyDocumentData.class)))
                .thenThrow(new IllegalStateException("PDF rendering failed"));

        assertThatCode(() -> listener.onVisitorStatusChanged(
                new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE)))
                .doesNotThrowAnyException();

        verify(emailService, never()).send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), anyList());
    }

    @Test
    void doesNotSendCertificateWhenNoBenefitsAssignedYet() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(List.of());

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        verifyNoInteractions(renderer, emailService);
    }

    @Test
    void doesNotSendCertificateWhenOnlySomeCatalogBenefitsAssigned() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(benefitService.listAll()).thenReturn(List.of(
                new BenefitResponse(catalogBenefitId, "Medical Expenses", new BigDecimal("20000.00"),
                        Instant.now(), Instant.now()),
                new BenefitResponse(UUID.randomUUID(), "Mental Illness", new BigDecimal("1000.00"),
                        Instant.now(), Instant.now())));
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        verifyNoInteractions(renderer, emailService);
        verify(visitorService, never()).markActivationEmailSent(any());
    }

    @Test
    void resendIfPendingSendsForActiveVisitorNeverEmailed() {
        stubFullSend();

        listener.resendActivationEmailIfPending(visitorId);

        verify(visitorService).markActivationEmailSent(visitorId);
    }

    @Test
    void resendIfPendingSkipsVisitorAlreadyEmailed() {
        Visitor emailed = sampleVisitor();
        emailed.setActivationEmailSentAt(Instant.now());
        when(visitorService.getEntityById(visitorId)).thenReturn(emailed);

        listener.resendActivationEmailIfPending(visitorId);

        verifyNoInteractions(renderer, emailService);
    }

    @Test
    void doesNotMarkActivationEmailSentWhenDeliveryFails() {
        stubFullSend();
        when(emailService.send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), anyList()))
                .thenReturn(false);

        listener.onVisitorBenefitAssigned(new VisitorBenefitAssignedEvent(visitorId));

        verify(visitorService, never()).markActivationEmailSent(any());
    }

    @Test
    void sendsCertificateWhenLastBenefitAssignedToActiveVisitorNotYetEmailed() {
        stubFullSend();

        listener.onVisitorBenefitAssigned(new VisitorBenefitAssignedEvent(visitorId));

        verify(emailService).send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), anyList());
        verify(visitorService).markActivationEmailSent(visitorId);
    }

    @Test
    void doesNotResendWhenBenefitAssignedAfterActivationEmailAlreadySent() {
        Visitor visitor = sampleVisitor();
        visitor.setActivationEmailSentAt(Instant.now());
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor);

        listener.onVisitorBenefitAssigned(new VisitorBenefitAssignedEvent(visitorId));

        verifyNoInteractions(renderer, emailService);
    }

    @Test
    void doesNotSendOnBenefitAssignmentWhenVisitorNotActive() {
        Visitor visitor = sampleVisitor();
        visitor.setVisitorStatus(VisitorStatus.PENDING);
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor);

        listener.onVisitorBenefitAssigned(new VisitorBenefitAssignedEvent(visitorId));

        verifyNoInteractions(renderer, emailService);
    }

    private void stubFullSend() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(sampleInsurer());
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());
    }

    @Test
    void sendsCertificateWhenVisitorCreatedAlreadyActive() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(sampleInsurer());
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(emailService).send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), anyList());
    }

    @Test
    void doesNotSendCertificateWhenCreatedVisitorNotActive() {
        Visitor pending = sampleVisitor();
        pending.setVisitorStatus(VisitorStatus.PENDING);
        when(visitorService.getEntityById(visitorId)).thenReturn(pending);

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verifyNoInteractions(policyService, visitorBenefitService, insurerService, renderer, emailService);
    }

    @Test
    void usesInsurerSmtpCredentialsWhenFullyConfigured() {
        Insurer insurer = sampleInsurer();
        insurer.setHost("smtp.acme.example");
        insurer.setPort(587);
        insurer.setNotificationEmail("notify@acme.example");
        insurer.setNotificationEmailPassword("s3cr3t");
        insurer.setEsignature("https://cdn.example/acme-signature.png");

        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(insurer);
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        ArgumentCaptor<PolicyDocumentData> dataCaptor = ArgumentCaptor.forClass(PolicyDocumentData.class);
        verify(renderer).renderPdf(dataCaptor.capture());
        assertThat(dataCaptor.getValue().esignatureUrl()).isEqualTo("https://cdn.example/acme-signature.png");

        ArgumentCaptor<SmtpCredentials> credentialsCaptor = ArgumentCaptor.forClass(SmtpCredentials.class);
        verify(emailService).send(
                credentialsCaptor.capture(),
                eq("notify@acme.example"),
                eq("jane.traveler@example.com"),
                anyList(),
                anyString(), anyString(), anyList());
        assertThat(credentialsCaptor.getValue())
                .isEqualTo(new SmtpCredentials("smtp.acme.example", 587, "notify@acme.example", "s3cr3t"));
    }

    @Test
    void attachesUnbrandedPolicyDocumentWhenInsurerHasNoLogoOrEsignature() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(sampleInsurer());
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        ArgumentCaptor<List<EmailAttachment>> attachmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService).send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), attachmentsCaptor.capture());
        assertThat(attachmentsCaptor.getValue())
                .extracting(EmailAttachment::filename)
                .contains("Policy Document.pdf");
        verify(renderer, never()).brandPolicyWording(any(), any(), any());
    }

    @Test
    void fallsBackToUnbrandedPolicyDocumentWhenBrandingFails() {
        Insurer insurer = sampleInsurer();
        insurer.setLogoUrl("https://cdn.example/acme.png");
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(insurer);
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());
        when(renderer.brandPolicyWording(any(byte[].class), anyString(), isNull()))
                .thenThrow(new IllegalStateException("logo fetch failed"));

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        ArgumentCaptor<List<EmailAttachment>> attachmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService).send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), attachmentsCaptor.capture());
        assertThat(attachmentsCaptor.getValue())
                .extracting(EmailAttachment::filename)
                .contains("Policy Document.pdf");
    }

    @Test
    void fallsBackToBrandedButUnfilledPolicyDocumentWhenAgreementFillingFails() {
        Insurer insurer = sampleInsurer();
        insurer.setLogoUrl("https://cdn.example/acme.png");
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(insurer);
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());
        when(renderer.brandPolicyWording(any(byte[].class), eq("https://cdn.example/acme.png"), isNull()))
                .thenReturn("%PDF-BRANDED".getBytes());
        when(renderer.fillPolicyAgreementDetails(eq("%PDF-BRANDED".getBytes()), anyString(), any(), anyString(),
                anyString(), any(LocalDate.class)))
                .thenThrow(new IllegalStateException("agreement fill failed"));

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        ArgumentCaptor<List<EmailAttachment>> attachmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService).send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), attachmentsCaptor.capture());
        assertThat(attachmentsCaptor.getValue())
                .filteredOn(attachment -> attachment.filename().equals("Policy Document.pdf"))
                .extracting(EmailAttachment::content)
                .singleElement()
                .isEqualTo("%PDF-BRANDED".getBytes());
    }

    @Test
    void attachesWelcomePackPdfAlongsideCertificateAndPolicyDocument() {
        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(sampleInsurer());
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        ArgumentCaptor<List<EmailAttachment>> attachmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService).send(any(), anyString(), anyString(), anyList(), anyString(), anyString(), attachmentsCaptor.capture());
        assertThat(attachmentsCaptor.getValue())
                .extracting(EmailAttachment::filename)
                .contains("Inbound-Travel-Health-Welcome-Pack.pdf");
    }

    @Test
    void fallsBackToGlobalMailerWhenInsurerCredentialsPartiallyConfigured() {
        Insurer insurer = sampleInsurer();
        insurer.setHost("smtp.acme.example");
        insurer.setPort(587);
        // notificationEmail / notificationEmailPassword left unset -> not fully configured

        when(visitorService.getEntityById(visitorId)).thenReturn(sampleVisitor());
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorBenefitService.listAllByVisitor(visitorId)).thenReturn(sampleBenefits());
        when(insurerService.getEntityById(insurerId)).thenReturn(insurer);
        when(renderer.renderPdf(any(PolicyDocumentData.class))).thenReturn("%PDF-1.4".getBytes());
        when(premiumReceiptService.calculateTotalPremium(anyInt())).thenReturn(new BigDecimal("44"));
        when(renderer.renderPremiumReceiptPdf(any(PremiumReceiptData.class))).thenReturn("%PDF-RECEIPT".getBytes());

        listener.onVisitorStatusChanged(new VisitorStatusChangedEvent(visitorId, VisitorStatus.ACTIVE));

        verify(emailService).send(
                isNull(),
                eq("no-reply@travelinsurance.example"),
                eq("jane.traveler@example.com"),
                anyList(),
                anyString(), anyString(), anyList());
    }
}
