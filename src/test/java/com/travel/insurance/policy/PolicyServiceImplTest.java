package com.travel.insurance.policy;

import com.travel.insurance.common.messaging.EventPublisher;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.policy.dto.PolicyRequest;
import com.travel.insurance.policy.dto.PolicyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private InsurerService insurerService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private final PolicyMapper policyMapper = new PolicyMapper();

    private PolicyServiceImpl policyService;

    private final UUID insurerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        policyService = new PolicyServiceImpl(
                policyRepository, policyMapper, insurerService, eventPublisher, applicationEventPublisher);
        when(insurerService.exists(insurerId)).thenReturn(true);
    }

    private PolicyRequest requestWithPeriod(PolicyType policyType, LocalDate start, LocalDate end) {
        return new PolicyRequest("POL-001", Set.of(insurerId), policyType, start, end, null);
    }

    @Test
    void createAcceptsShortestBoundaryForSingleEntryUpTo30Days() {
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyRequest request = requestWithPeriod(PolicyType.SINGLE_ENTRY_UP_TO_30_DAYS,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 30));

        PolicyResponse response = policyService.create(request);

        assertThat(response.policyType()).isEqualTo(PolicyType.SINGLE_ENTRY_UP_TO_30_DAYS);
        verify(policyRepository).save(any(Policy.class));
    }

    @Test
    void createRejectsCoverPeriodExceedingSingleEntryUpTo30Days() {
        PolicyRequest request = requestWithPeriod(PolicyType.SINGLE_ENTRY_UP_TO_30_DAYS,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

        assertThatThrownBy(() -> policyService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SINGLE_ENTRY_UP_TO_30_DAYS");
        verify(policyRepository, never()).save(any());
    }

    @Test
    void createAcceptsBoundariesForSingleEntry31To60Days() {
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyRequest request = requestWithPeriod(PolicyType.SINGLE_ENTRY_31_TO_60_DAYS,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1));

        PolicyResponse response = policyService.create(request);

        assertThat(response.policyType()).isEqualTo(PolicyType.SINGLE_ENTRY_31_TO_60_DAYS);
    }

    @Test
    void createRejectsCoverPeriodBelowSingleEntry31To60DaysMinimum() {
        PolicyRequest request = requestWithPeriod(PolicyType.SINGLE_ENTRY_31_TO_60_DAYS,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15));

        assertThatThrownBy(() -> policyService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SINGLE_ENTRY_31_TO_60_DAYS");
        verify(policyRepository, never()).save(any());
    }

    @Test
    void createAcceptsIpmiUpTo12Months() {
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyRequest request = requestWithPeriod(PolicyType.IPMI_61_DAYS_TO_12_MONTHS,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        PolicyResponse response = policyService.create(request);

        assertThat(response.policyType()).isEqualTo(PolicyType.IPMI_61_DAYS_TO_12_MONTHS);
    }

    @Test
    void createRejectsCoverPeriodBelowIpmiMinimum() {
        PolicyRequest request = requestWithPeriod(PolicyType.IPMI_61_DAYS_TO_12_MONTHS,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

        assertThatThrownBy(() -> policyService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IPMI_61_DAYS_TO_12_MONTHS");
        verify(policyRepository, never()).save(any());
    }

    @Test
    void createPublishesActivationEventWhenStatusIsActive() {
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        PolicyRequest request = new PolicyRequest("POL-001", Set.of(insurerId),
                PolicyType.IPMI_61_DAYS_TO_12_MONTHS, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                PolicyStatus.ACTIVE);

        policyService.create(request);

        ArgumentCaptor<PolicyActivatingEvent> captor = ArgumentCaptor.forClass(PolicyActivatingEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().policyId()).isNotNull();
    }

    @Test
    void createDoesNotPublishActivationEventWhenStatusIsDraft() {
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyRequest request = requestWithPeriod(PolicyType.IPMI_61_DAYS_TO_12_MONTHS,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        policyService.create(request);

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createRollsBackWhenActivationGateRejectsThePolicy() {
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        doThrow(new IllegalStateException("Policy cannot be activated: missing benefits"))
                .when(applicationEventPublisher).publishEvent(any(PolicyActivatingEvent.class));

        PolicyRequest request = new PolicyRequest("POL-001", Set.of(insurerId),
                PolicyType.IPMI_61_DAYS_TO_12_MONTHS, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                PolicyStatus.ACTIVE);

        assertThatThrownBy(() -> policyService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing benefits");
        verify(eventPublisher, never()).publish(any(), any());
    }
}
