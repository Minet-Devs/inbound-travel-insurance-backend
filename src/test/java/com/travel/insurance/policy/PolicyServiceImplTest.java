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

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    }

    private PolicyRequest requestWithType(PolicyType policyType, PolicyStatus status) {
        return new PolicyRequest("POL-001", Set.of(insurerId), policyType, status);
    }

    @Test
    void createRejectsUnknownInsurer() {
        when(insurerService.exists(insurerId)).thenReturn(false);

        PolicyRequest request = requestWithType(PolicyType.IPMI_61_DAYS_TO_12_MONTHS, null);

        assertThatThrownBy(() -> policyService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(insurerId.toString());
        verify(policyRepository, never()).save(any());
    }

    @Test
    void createSavesPolicyWithPolicyType() {
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyRequest request = requestWithType(PolicyType.SINGLE_ENTRY_UP_TO_30_DAYS, null);

        PolicyResponse response = policyService.create(request);

        assertThat(response.policyType()).isEqualTo(PolicyType.SINGLE_ENTRY_UP_TO_30_DAYS);
        verify(policyRepository).save(any(Policy.class));
    }

    @Test
    void createPublishesActivationEventWhenStatusIsActive() {
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        PolicyRequest request = requestWithType(PolicyType.IPMI_61_DAYS_TO_12_MONTHS, PolicyStatus.ACTIVE);

        policyService.create(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());
        PolicyActivatingEvent activating = captor.getAllValues().stream()
                .filter(PolicyActivatingEvent.class::isInstance)
                .map(PolicyActivatingEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(activating.policyId()).isNotNull();
    }

    @Test
    void createPublishesPolicyCreatedEventSoBenefitsAreProvisioned() {
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        PolicyRequest request = requestWithType(PolicyType.IPMI_61_DAYS_TO_12_MONTHS, null);

        policyService.create(request);

        ArgumentCaptor<PolicyCreatedEvent> captor = ArgumentCaptor.forClass(PolicyCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().policyId()).isNotNull();
    }

    @Test
    void createDoesNotPublishActivationEventWhenStatusIsDraft() {
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyRequest request = requestWithType(PolicyType.IPMI_61_DAYS_TO_12_MONTHS, null);

        policyService.create(request);

        verify(applicationEventPublisher).publishEvent(any(PolicyCreatedEvent.class));
        verify(applicationEventPublisher, never()).publishEvent(any(PolicyActivatingEvent.class));
    }

    @Test
    void createRollsBackWhenActivationGateRejectsThePolicy() {
        when(insurerService.exists(insurerId)).thenReturn(true);
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        // create also publishes PolicyCreatedEvent first; only the activation
        // event should trip the gate, so keep the throwing stub lenient.
        lenient().doThrow(new IllegalStateException("Policy cannot be activated: missing benefits"))
                .when(applicationEventPublisher).publishEvent(any(PolicyActivatingEvent.class));

        PolicyRequest request = requestWithType(PolicyType.IPMI_61_DAYS_TO_12_MONTHS, PolicyStatus.ACTIVE);

        assertThatThrownBy(() -> policyService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing benefits");
        verify(eventPublisher, never()).publish(any(), any());
    }
}
