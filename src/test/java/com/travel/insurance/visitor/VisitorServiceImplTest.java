package com.travel.insurance.visitor;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.visitor.dto.VisitorRequest;
import com.travel.insurance.visitor.dto.VisitorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private ApplicationEventPublisher eventPublisher;

    private final VisitorMapper visitorMapper = new VisitorMapper();

    private VisitorServiceImpl visitorService;

    private UUID policyId;
    private VisitorRequest request;

    @BeforeEach
    void setUp() {
        visitorService = new VisitorServiceImpl(
                visitorRepository, visitorMapper, policyService, eventPublisher);
        policyId = UUID.randomUUID();
        request = new VisitorRequest(
                policyId,
                "Jane Traveler",
                "P1234567",
                LocalDate.of(1990, 5, 12),
                Gender.FEMALE,
                "Germany",
                "jane.traveler@example.com",
                "+254700000000",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 11, 1),
                MaritalStatus.SINGLE,
                "John Traveler",
                "+254711111111");
    }

    @Test
    void createSavesWhenPolicyFreeAndPassportUnique() {
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(visitorRepository.existsByPassportNumberIgnoreCase("P1234567")).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitorResponse response = visitorService.create(request);

        assertThat(response.fullName()).isEqualTo("Jane Traveler");
        assertThat(response.policyId()).isEqualTo(policyId);
        verify(visitorRepository).save(any(Visitor.class));
        verify(eventPublisher).publishEvent(any(VisitorCreatedEvent.class));
    }

    @Test
    void createAllowsSecondVisitorOnSamePolicy() {
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(visitorRepository.existsByPassportNumberIgnoreCase("P7654321")).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitorRequest secondVisitor = new VisitorRequest(
                policyId,
                "John Traveler",
                "P7654321",
                LocalDate.of(1988, 2, 3),
                Gender.MALE,
                "Germany",
                "john.traveler@example.com",
                "+254722222222",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 11, 1),
                MaritalStatus.MARRIED,
                "Jane Traveler",
                "+254700000000");

        VisitorResponse response = visitorService.create(secondVisitor);

        assertThat(response.policyId()).isEqualTo(policyId);
        verify(visitorRepository).save(any(Visitor.class));
    }

    @Test
    void createRejectsDuplicatePassportNumber() {
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(visitorRepository.existsByPassportNumberIgnoreCase("P1234567")).thenReturn(true);

        assertThatThrownBy(() -> visitorService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("P1234567");
        verify(visitorRepository, never()).save(any());
    }

    @Test
    void getByPassportNumberReturnsVisitorKyc() {
        Visitor existing = visitorMapper.toEntity(request);
        when(visitorRepository.findByPassportNumberIgnoreCase("P1234567"))
                .thenReturn(Optional.of(existing));

        VisitorResponse response = visitorService.getByPassportNumber("P1234567");

        assertThat(response.passportNumber()).isEqualTo("P1234567");
        assertThat(response.fullName()).isEqualTo("Jane Traveler");
        assertThat(response.policyId()).isEqualTo(policyId);
    }

    @Test
    void getByPassportNumberThrowsWhenUnknown() {
        when(visitorRepository.findByPassportNumberIgnoreCase("UNKNOWN"))
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
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(visitorRepository.existsByPassportNumberIgnoreCaseAndIdNot("P1234567", id)).thenReturn(true);

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
        when(policyService.getEntityById(policyId)).thenReturn(new Policy());
        when(visitorRepository.existsByPassportNumberIgnoreCaseAndIdNot("P1234567", id)).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VisitorResponse response = visitorService.update(id, request);

        assertThat(response.passportNumber()).isEqualTo("P1234567");
    }
}
