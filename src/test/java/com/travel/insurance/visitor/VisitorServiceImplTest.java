package com.travel.insurance.visitor;

import com.travel.insurance.common.crypto.BlindIndexService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerRepository;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.visitor.dto.VisitorEntryExitUpdate;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import com.travel.insurance.visitor.dto.VisitorStatusUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorServiceImplTest {

    @Mock
    private VisitorRepository visitorRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private InsurerRepository insurerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BlindIndexService blindIndexService;

    @Mock
    private CertificateSerialNumberGenerator certificateSerialNumberGenerator;

    private final VisitorMapper visitorMapper = new VisitorMapper();

    private VisitorServiceImpl visitorService;

    private UUID policyId;
    private UUID insurerId;
    private VisitorRequest request;

    private static String hashOf(String passportNumber) {
        return "HASH:" + passportNumber;
    }

    @BeforeEach
    void setUp() {
        visitorService = new VisitorServiceImpl(
                visitorRepository, visitorMapper, policyService, insurerRepository, eventPublisher,
                blindIndexService, certificateSerialNumberGenerator);
        lenient().when(blindIndexService.hmac(anyString()))
                .thenAnswer(invocation -> hashOf(invocation.getArgument(0)));
        policyId = UUID.randomUUID();
        insurerId = UUID.randomUUID();
        Insurer insurer = new Insurer();
        insurer.setId(insurerId);
        insurer.setName("Minet Insurance");
        insurer.setPolicyToken(1000L);
        lenient().when(insurerRepository.findById(insurerId)).thenReturn(Optional.of(insurer));
        request = new VisitorRequest(
                policyId,
                "Jane Traveler",
                "P1234567",
                LocalDate.of(1990, 5, 12),
                Gender.FEMALE,
                "Germany",
                "12 Example Street, Berlin",
                "jane.traveler@example.com",
                "+254700000000",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 11, 1),
                MaritalStatus.SINGLE,
                "Tourism",
                "https://storage.example.com/photos/jane.jpg",
                null,
                "John Traveler",
                "+254711111111",
                null,
                null,
                null,
                null,
                null);
    }

    private Policy samplePolicy() {
        Policy policy = new Policy();
        policy.setInsurerId(insurerId);
        return policy;
    }

    @Test
    void createSavesWhenPolicyFreeAndPassportUnique() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHash(hashOf("P1234567"))).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitorResponse response = visitorService.create(request);

        assertThat(response.fullName()).isEqualTo("Jane Traveler");
        assertThat(response.policyId()).isEqualTo(policyId);
        assertThat(response.insurerId()).isEqualTo(insurerId);
        assertThat(response.visitorStatus()).isEqualTo(VisitorStatus.ACTIVE);
        assertThat(response.policyExpiryDate()).isEqualTo(response.dateIn().plusDays(365));
        verify(visitorRepository).save(any(Visitor.class));
        verify(eventPublisher).publishEvent(any(VisitorCreatedEvent.class));
    }

    @Test
    void createSetsEmailHashForEmailLookup() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHash(hashOf("P1234567"))).thenReturn(false);
        ArgumentCaptor<Visitor> captor = ArgumentCaptor.forClass(Visitor.class);
        when(visitorRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        visitorService.create(request);

        assertThat(captor.getValue().getEmailHash()).isEqualTo(hashOf("jane.traveler@example.com"));
    }

    @Test
    void createMintsCertificateSerialNumberForActiveVisitor() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHash(hashOf("P1234567"))).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(certificateSerialNumberGenerator.next("Minet Insurance")).thenReturn("MINET-2026-000001");

        VisitorResponse response = visitorService.create(request);

        assertThat(response.certificateSerialNumber()).isEqualTo("MINET-2026-000001");
    }

    @Test
    void createAllowsSecondVisitorOnSamePolicy() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHash(hashOf("P7654321"))).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitorRequest secondVisitor = new VisitorRequest(
                policyId,
                "John Traveler",
                "P7654321",
                LocalDate.of(1988, 2, 3),
                Gender.MALE,
                "Germany",
                "12 Example Street, Berlin",
                "john.traveler@example.com",
                "+254722222222",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 11, 1),
                MaritalStatus.MARRIED,
                "Business",
                "https://storage.example.com/photos/john.jpg",
                "Diabetes, requires insulin",
                "Jane Traveler",
                "+254700000000",
                null,
                null,
                null,
                null,
                null);

        VisitorResponse response = visitorService.create(secondVisitor);

        assertThat(response.policyId()).isEqualTo(policyId);
        verify(visitorRepository).save(any(Visitor.class));
    }

    @Test
    void createRejectsDuplicatePassportNumber() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHash(hashOf("P1234567"))).thenReturn(true);

        assertThatThrownBy(() -> visitorService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("P1234567");
        verify(visitorRepository, never()).save(any());
    }

    private VisitorRequest requestWithTravelPeriod(LocalDate dateIn, LocalDate dateOut) {
        return new VisitorRequest(
                policyId, "Jane Traveler", "P1234567", LocalDate.of(1990, 5, 12), Gender.FEMALE,
                "Germany", "12 Example Street, Berlin", "jane.traveler@example.com", "+254700000000",
                dateIn, dateOut, MaritalStatus.SINGLE, "Tourism",
                "https://storage.example.com/photos/jane.jpg", null, "John Traveler", "+254711111111",
                null, null, null, null, null);
    }

    @Test
    void createRejectsDateOutBeforeDateIn() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());

        VisitorRequest reversed = requestWithTravelPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> visitorService.create(reversed))
                .isInstanceOf(IllegalArgumentException.class);
        verify(visitorRepository, never()).save(any());
    }

    @Test
    void createAcceptsSameDayTravelPeriod() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHash(hashOf("P1234567"))).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitorRequest oneDay = requestWithTravelPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        VisitorResponse response = visitorService.create(oneDay);

        assertThat(response.dateIn()).isEqualTo(LocalDate.of(2026, 1, 1));
        verify(visitorRepository).save(any(Visitor.class));
    }

    @Test
    void createAcceptsUpToTwelveMonthsBoundary() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHash(hashOf("P1234567"))).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitorRequest threeSixtyFiveDays = requestWithTravelPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        VisitorResponse response = visitorService.create(threeSixtyFiveDays);

        assertThat(response.dateIn()).isEqualTo(LocalDate.of(2026, 1, 1));
        verify(visitorRepository).save(any(Visitor.class));
    }

    @Test
    void createRejectsTravelPeriodExceedingTwelveMonths() {
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());

        VisitorRequest tooLong = requestWithTravelPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        assertThatThrownBy(() -> visitorService.create(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("365");
        verify(visitorRepository, never()).save(any());
    }

    @Test
    void getByPassportNumberReturnsVisitorKyc() {
        Visitor existing = visitorMapper.toEntity(request);
        when(visitorRepository.findByPassportNumberHash(hashOf("P1234567")))
                .thenReturn(Optional.of(existing));

        VisitorResponse response = visitorService.getByPassportNumber("P1234567");

        assertThat(response.passportNumber()).isEqualTo("P1234567");
        assertThat(response.fullName()).isEqualTo("Jane Traveler");
        assertThat(response.policyId()).isEqualTo(policyId);
    }

    @Test
    void getByPassportNumberThrowsWhenUnknown() {
        when(visitorRepository.findByPassportNumberHash(hashOf("UNKNOWN")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> visitorService.getByPassportNumber("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void updateRejectsPassportUsedByAnotherVisitor() {
        UUID id = UUID.randomUUID();
        Visitor existing = visitorMapper.toEntity(request);
        when(visitorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHashAndIdNot(hashOf("P1234567"), id)).thenReturn(true);

        assertThatThrownBy(() -> visitorService.update(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("P1234567");
        verify(visitorRepository, never()).save(any());
    }

    @Test
    void updateAllowsKeepingOwnPassportNumber() {
        UUID id = UUID.randomUUID();
        Visitor existing = visitorMapper.toEntity(request);
        when(visitorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(policyService.getEntityById(policyId)).thenReturn(samplePolicy());
        when(visitorRepository.existsByPassportNumberHashAndIdNot(hashOf("P1234567"), id)).thenReturn(false);

        VisitorResponse response = visitorService.update(id, request);

        assertThat(response.passportNumber()).isEqualTo("P1234567");
    }

    @Test
    void listWithoutFilterReturnsAllVisitors() {
        Pageable pageable = PageRequest.of(0, 10);
        Visitor existing = visitorMapper.toEntity(request);
        existing.setInsurerId(insurerId);
        when(visitorRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(existing)));

        Page<VisitorResponse> result = visitorService.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(visitorRepository, never()).findByInsurerId(any(), any());
    }

    @Test
    void listFiltersByInsurerId() {
        Pageable pageable = PageRequest.of(0, 10);
        Visitor existing = visitorMapper.toEntity(request);
        existing.setInsurerId(insurerId);
        when(visitorRepository.findByInsurerId(insurerId, pageable)).thenReturn(new PageImpl<>(List.of(existing)));

        Page<VisitorResponse> result = visitorService.list(insurerId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).insurerId()).isEqualTo(insurerId);
        verify(visitorRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void updateResolvesInsurerIdFromPolicy() {
        UUID id = UUID.randomUUID();
        Visitor existing = visitorMapper.toEntity(request);
        existing.setInsurerId(insurerId);
        UUID newInsurerId = UUID.randomUUID();
        Policy newPolicy = new Policy();
        newPolicy.setInsurerId(newInsurerId);
        when(visitorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(policyService.getEntityById(policyId)).thenReturn(newPolicy);
        when(visitorRepository.existsByPassportNumberHashAndIdNot(hashOf("P1234567"), id)).thenReturn(false);

        VisitorResponse response = visitorService.update(id, request);

        assertThat(response.insurerId()).isEqualTo(newInsurerId);
    }

    @Test
    void updateVisitorStatusAppliesAllowedTransitionAndPublishesEvent() {
        UUID id = UUID.randomUUID();
        Visitor existing = visitorMapper.toEntity(request);
        existing.setVisitorStatus(VisitorStatus.PENDING);
        existing.setInsurerId(insurerId);
        when(visitorRepository.findById(id)).thenReturn(Optional.of(existing));

        visitorService.updateVisitorStatus(id, new VisitorStatusUpdate(VisitorStatus.ACTIVE));

        assertThat(existing.getVisitorStatus()).isEqualTo(VisitorStatus.ACTIVE);
        ArgumentCaptor<VisitorStatusChangedEvent> captor =
                ArgumentCaptor.forClass(VisitorStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().newStatus()).isEqualTo(VisitorStatus.ACTIVE);
    }

    @Test
    void updateVisitorStatusMintsCertificateSerialNumberOnFirstActivation() {
        UUID id = UUID.randomUUID();
        Visitor existing = visitorMapper.toEntity(request);
        existing.setVisitorStatus(VisitorStatus.PENDING);
        existing.setInsurerId(insurerId);
        when(visitorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(certificateSerialNumberGenerator.next("Minet Insurance")).thenReturn("MINET-2026-000001");

        VisitorResponse response = visitorService.updateVisitorStatus(id, new VisitorStatusUpdate(VisitorStatus.ACTIVE));

        assertThat(response.certificateSerialNumber()).isEqualTo("MINET-2026-000001");
        assertThat(existing.getCertificateSerialNumber()).isEqualTo("MINET-2026-000001");
    }

    @Test
    void updateVisitorStatusReactivationReusesExistingCertificateSerialNumber() {
        UUID id = UUID.randomUUID();
        Visitor existing = visitorMapper.toEntity(request);
        existing.setVisitorStatus(VisitorStatus.SUSPENDED);
        existing.setInsurerId(insurerId);
        existing.setCertificateSerialNumber("MINET-2026-000042");
        when(visitorRepository.findById(id)).thenReturn(Optional.of(existing));

        VisitorResponse response = visitorService.updateVisitorStatus(id, new VisitorStatusUpdate(VisitorStatus.ACTIVE));

        assertThat(response.certificateSerialNumber()).isEqualTo("MINET-2026-000042");
        verify(certificateSerialNumberGenerator, never()).next(anyString());
    }

    @Test
    void updateVisitorStatusRejectsInvalidTransition() {
        UUID id = UUID.randomUUID();
        Visitor existing = visitorMapper.toEntity(request);
        existing.setVisitorStatus(VisitorStatus.DEACTIVATED);
        when(visitorRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> visitorService.updateVisitorStatus(
                id, new VisitorStatusUpdate(VisitorStatus.ACTIVE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEACTIVATED")
                .hasMessageContaining("ACTIVE");
        assertThat(existing.getVisitorStatus()).isEqualTo(VisitorStatus.DEACTIVATED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateVisitorStatusRejectsSameStatus() {
        UUID id = UUID.randomUUID();
        Visitor existing = visitorMapper.toEntity(request);
        when(visitorRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> visitorService.updateVisitorStatus(
                id, new VisitorStatusUpdate(VisitorStatus.PENDING)))
                .isInstanceOf(IllegalStateException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateVisitorStatusByPassportNumberAppliesAllowedTransitionAndPublishesEvent() {
        Visitor existing = visitorMapper.toEntity(request);
        existing.setVisitorStatus(VisitorStatus.PENDING);
        existing.setInsurerId(insurerId);
        when(visitorRepository.findByPassportNumberHash(hashOf("P1234567")))
                .thenReturn(Optional.of(existing));

        visitorService.updateVisitorStatusByPassportNumber(
                "P1234567", new VisitorStatusUpdate(VisitorStatus.ACTIVE));

        assertThat(existing.getVisitorStatus()).isEqualTo(VisitorStatus.ACTIVE);
        ArgumentCaptor<VisitorStatusChangedEvent> captor =
                ArgumentCaptor.forClass(VisitorStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().newStatus()).isEqualTo(VisitorStatus.ACTIVE);
    }

    @Test
    void updateVisitorStatusByPassportNumberRejectsInvalidTransition() {
        Visitor existing = visitorMapper.toEntity(request);
        existing.setVisitorStatus(VisitorStatus.DEACTIVATED);
        when(visitorRepository.findByPassportNumberHash(hashOf("P1234567")))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> visitorService.updateVisitorStatusByPassportNumber(
                "P1234567", new VisitorStatusUpdate(VisitorStatus.ACTIVE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEACTIVATED")
                .hasMessageContaining("ACTIVE");
        assertThat(existing.getVisitorStatus()).isEqualTo(VisitorStatus.DEACTIVATED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateVisitorStatusByPassportNumberThrowsWhenVisitorUnknown() {
        when(visitorRepository.findByPassportNumberHash(hashOf("UNKNOWN")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> visitorService.updateVisitorStatusByPassportNumber(
                "UNKNOWN", new VisitorStatusUpdate(VisitorStatus.ACTIVE)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateEntryExitByPassportNumberSetsEntryTimestampOnly() {
        Visitor existing = visitorMapper.toEntity(request);
        when(visitorRepository.findByPassportNumberHash(hashOf("P1234567")))
                .thenReturn(Optional.of(existing));
        Instant entry = Instant.parse("2026-08-01T10:00:00Z");

        VisitorResponse response = visitorService.updateEntryExitByPassportNumber(
                "P1234567", new VisitorEntryExitUpdate(entry, null));

        assertThat(response.entryTimestamp()).isEqualTo(entry);
        assertThat(response.exitTimestamp()).isNull();
    }

    @Test
    void updateEntryExitByPassportNumberSetsExitTimestampOnly() {
        Visitor existing = visitorMapper.toEntity(request);
        existing.setEntryTimestamp(Instant.parse("2026-08-01T10:00:00Z"));
        when(visitorRepository.findByPassportNumberHash(hashOf("P1234567")))
                .thenReturn(Optional.of(existing));
        Instant exit = Instant.parse("2026-08-10T10:00:00Z");

        VisitorResponse response = visitorService.updateEntryExitByPassportNumber(
                "P1234567", new VisitorEntryExitUpdate(null, exit));

        assertThat(response.entryTimestamp()).isEqualTo(Instant.parse("2026-08-01T10:00:00Z"));
        assertThat(response.exitTimestamp()).isEqualTo(exit);
    }

    @Test
    void updateEntryExitByPassportNumberRejectsBothTimestampsProvided() {
        assertThatThrownBy(() -> visitorService.updateEntryExitByPassportNumber(
                "P1234567", new VisitorEntryExitUpdate(Instant.now(), Instant.now())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(visitorRepository, never()).findByPassportNumberHash(anyString());
    }

    @Test
    void updateEntryExitByPassportNumberRejectsNeitherTimestampProvided() {
        assertThatThrownBy(() -> visitorService.updateEntryExitByPassportNumber(
                "P1234567", new VisitorEntryExitUpdate(null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(visitorRepository, never()).findByPassportNumberHash(anyString());
    }

    @Test
    void updateEntryExitByPassportNumberThrowsWhenVisitorUnknown() {
        when(visitorRepository.findByPassportNumberHash(hashOf("UNKNOWN")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> visitorService.updateEntryExitByPassportNumber(
                "UNKNOWN", new VisitorEntryExitUpdate(Instant.now(), null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateVisitorStatusThrowsWhenVisitorUnknown() {
        UUID id = UUID.randomUUID();
        when(visitorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> visitorService.updateVisitorStatus(
                id, new VisitorStatusUpdate(VisitorStatus.ACTIVE)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void findByEmailReturnsVisitorMatchingHash() {
        Visitor visitor = new Visitor();
        visitor.setId(UUID.randomUUID());
        when(visitorRepository.findFirstByEmailHashOrderByCreatedDateDesc(hashOf("jane.traveler@example.com")))
                .thenReturn(Optional.of(visitor));

        Optional<Visitor> found = visitorService.findByEmail("jane.traveler@example.com");

        assertThat(found).contains(visitor);
    }

    @Test
    void findByEmailReturnsEmptyWhenUnknown() {
        when(visitorRepository.findFirstByEmailHashOrderByCreatedDateDesc(hashOf("unknown@example.com")))
                .thenReturn(Optional.empty());

        assertThat(visitorService.findByEmail("unknown@example.com")).isEmpty();
    }
}
