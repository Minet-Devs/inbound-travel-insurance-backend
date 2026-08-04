package com.travel.insurance.biometric;

import com.travel.insurance.biometric.client.EkYcClient;
import com.travel.insurance.biometric.client.EkYcCreateRequest;
import com.travel.insurance.biometric.client.EkYcEmbededResponse;
import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.biometric.dto.BiometricVerificationRequest;
import com.travel.insurance.biometric.dto.BiometricVerificationResponse;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.config.EkYcProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiometricVerificationServiceImplTest {

    @Mock
    private BiometricVerificationRepository repository;

    @Mock
    private EkYcClient ekycClient;

    @Mock
    private EkYcProperties properties;

    @Mock
    private EventPublisher eventPublisher;

    private final BiometricVerificationMapper mapper = new BiometricVerificationMapper();

    private BiometricVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BiometricVerificationServiceImpl(repository, mapper, ekycClient, properties, eventPublisher);
    }

    @Test
    void createTriggersEkYcAndStoresEmbededDetails() {
        when(properties.getNotificationCallbackUrl())
                .thenReturn("https://host.example/api/v1/webhooks/biometric-verification");
        when(repository.save(any(BiometricVerification.class))).thenAnswer(invocation -> {
            BiometricVerification verification = invocation.getArgument(0);
            if (verification.getId() == null) {
                verification.setId(UUID.randomUUID());
            }
            return verification;
        });
        when(ekycClient.createEmbededRequest(any(EkYcCreateRequest.class)))
                .thenReturn(new EkYcEmbededResponse("ekyc-req-1", "token-1", "2026-08-04T12:00:00Z",
                        "https://ekyc.example/embeded?request_id=ekyc-req-1"));

        BiometricVerificationResponse response = service.create(
                new BiometricVerificationRequest("39289507", "citizen", "VMI-POL-001", "WS-NRB-014"));

        assertThat(response.status()).isEqualTo(BiometricVerificationStatus.PENDING);
        assertThat(response.ekycRequestId()).isEqualTo("ekyc-req-1");
        assertThat(response.embededToken()).isEqualTo("token-1");
        assertThat(response.requestUrl()).contains("ekyc-req-1");

        ArgumentCaptor<EkYcCreateRequest> captor = ArgumentCaptor.forClass(EkYcCreateRequest.class);
        verify(ekycClient).createEmbededRequest(captor.capture());
        EkYcCreateRequest sent = captor.getValue();
        assertThat(sent.subjectIdNumber()).isEqualTo("39289507");
        assertThat(sent.workstationId()).isEqualTo("WS-NRB-014");
        assertThat(sent.notificationCallbackUrl())
                .isEqualTo("https://host.example/api/v1/webhooks/biometric-verification");
        assertThat(sent.relyingPartyRequestId()).isEqualTo(response.id().toString());
        verify(repository, org.mockito.Mockito.times(2)).save(any(BiometricVerification.class));
    }

    @Test
    void handleCallbackResolvesPendingVerificationAndPublishesEvent() {
        BiometricVerification verification = pendingVerification();
        verification.setEkycRequestId("ekyc-req-1");
        when(repository.findByEkycRequestId("ekyc-req-1")).thenReturn(Optional.of(verification));
        when(repository.save(any(BiometricVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handleCallback(new BiometricCallbackPayload("ekyc-req-1", verification.getId().toString(),
                "accepted", "match", "DICP2000", 3, "hash"));

        assertThat(verification.getStatus()).isEqualTo(BiometricVerificationStatus.ACCEPTED);
        assertThat(verification.getResult()).isEqualTo("match");
        assertThat(verification.getRemainingAttempts()).isEqualTo(3);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("biometric-verification.resolved"),
                org.mockito.ArgumentMatchers.any(Map.class));
    }

    @Test
    void handleCallbackIgnoresDuplicateCallback() {
        BiometricVerification verification = pendingVerification();
        verification.setEkycRequestId("ekyc-req-1");
        verification.setStatus(BiometricVerificationStatus.ACCEPTED);
        when(repository.findByEkycRequestId("ekyc-req-1")).thenReturn(Optional.of(verification));

        service.handleCallback(new BiometricCallbackPayload("ekyc-req-1", verification.getId().toString(),
                "accepted", "match", "DICP2000", 3, "hash"));

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void handleCallbackThrowsForUnknownEkYcRequestId() {
        when(repository.findByEkycRequestId("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleCallback(
                new BiometricCallbackPayload("unknown-id", "rp-1", "accepted", "match", "DICP2000", 3, "hash")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resendProxiesToEkYcWhenPending() {
        BiometricVerification verification = pendingVerification();
        verification.setEkycRequestId("ekyc-req-1");
        when(repository.findById(verification.getId())).thenReturn(Optional.of(verification));
        when(ekycClient.resendCallback("ekyc-req-1")).thenReturn(HttpStatusCode.valueOf(200));

        HttpStatusCode status = service.resend(verification.getId());

        assertThat(status.value()).isEqualTo(200);
        verify(ekycClient).resendCallback("ekyc-req-1");
    }

    @Test
    void resendRejectsResolvedVerification() {
        BiometricVerification verification = pendingVerification();
        verification.setStatus(BiometricVerificationStatus.ACCEPTED);
        when(repository.findById(verification.getId())).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.resend(verification.getId()))
                .isInstanceOf(IllegalStateException.class);
        verify(ekycClient, never()).resendCallback(any());
    }

    @Test
    void resendRejectsVerificationWithoutEkYcRequestId() {
        BiometricVerification verification = pendingVerification();
        when(repository.findById(verification.getId())).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.resend(verification.getId()))
                .isInstanceOf(IllegalStateException.class);
        verify(ekycClient, never()).resendCallback(any());
    }

    private BiometricVerification pendingVerification() {
        BiometricVerification verification = new BiometricVerification();
        verification.setId(UUID.randomUUID());
        verification.setSubjectIdNumber("39289507");
        verification.setSubjectIdType("citizen");
        verification.setPolicyNumber("VMI-POL-001");
        verification.setWorkstationId("WS-NRB-014");
        verification.setStatus(BiometricVerificationStatus.PENDING);
        return verification;
    }
}
