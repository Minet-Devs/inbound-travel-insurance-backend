package com.travel.insurance.policy;

import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerRepository;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyConsumptionListenerTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private InsurerRepository insurerRepository;

    @Mock
    private OrganizationService organizationService;

    private PolicyConsumptionListener listener;

    private final UUID policyId = UUID.randomUUID();
    private final UUID visitorId = UUID.randomUUID();
    private final UUID insurerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new PolicyConsumptionListener(policyRepository, insurerRepository, organizationService);
    }

    private Insurer insurerWithToken(Long token, UUID organizationId) {
        Insurer insurer = new Insurer();
        insurer.setId(insurerId);
        insurer.setName("Minet Insurance");
        insurer.setPolicyToken(token);
        insurer.setOrganizationId(organizationId);
        return insurer;
    }

    private void givenPolicyAndInsurer(Insurer insurer) {
        Policy policy = new Policy();
        policy.setId(policyId);
        policy.setInsurerId(insurerId);
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(insurerRepository.findById(insurerId)).thenReturn(Optional.of(insurer));
    }

    @Test
    void onVisitorCreatedConsumesOneTokenAtomicallyInsteadOfSavingTheEntity() {
        givenPolicyAndInsurer(insurerWithToken(1000L, null));
        when(insurerRepository.consumeOnePolicyToken(insurerId)).thenReturn(1);
        when(insurerRepository.findPolicyTokenById(insurerId)).thenReturn(Optional.of(999L));

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(insurerRepository, times(1)).consumeOnePolicyToken(insurerId);
        verify(insurerRepository, never()).save(any(Insurer.class));
    }

    @Test
    void onVisitorCreatedDoesNothingMoreWhenQuotaIsNull() {
        givenPolicyAndInsurer(insurerWithToken(null, UUID.randomUUID()));
        when(insurerRepository.consumeOnePolicyToken(insurerId)).thenReturn(0);

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(insurerRepository, never()).save(any(Insurer.class));
        verify(insurerRepository, never()).findPolicyTokenById(any());
        verify(organizationService, never()).syncPolicyToken(any(), any());
    }

    @Test
    void onVisitorCreatedDoesNothingMoreWhenQuotaIsExhausted() {
        givenPolicyAndInsurer(insurerWithToken(0L, UUID.randomUUID()));
        when(insurerRepository.consumeOnePolicyToken(insurerId)).thenReturn(0);

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(insurerRepository, never()).save(any(Insurer.class));
        verify(organizationService, never()).syncPolicyToken(any(), any());
    }

    @Test
    void onVisitorCreatedHandlesQuotaExhaustedByAConcurrentCreate() {
        // The session's entity still shows 1, but another transaction took the last token first.
        givenPolicyAndInsurer(insurerWithToken(1L, UUID.randomUUID()));
        when(insurerRepository.consumeOnePolicyToken(insurerId)).thenReturn(0);

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(organizationService, never()).syncPolicyToken(any(), any());
    }

    @Test
    void onVisitorCreatedThrowsExceptionWhenPolicyNotFound() {
        when(policyRepository.findById(policyId)).thenReturn(Optional.empty());

        VisitorCreatedEvent event = new VisitorCreatedEvent(visitorId, policyId);
        assertThatThrownBy(() -> listener.onVisitorCreated(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Policy not found");
        verify(insurerRepository, never()).consumeOnePolicyToken(any());
    }

    @Test
    void onVisitorCreatedThrowsExceptionWhenInsurerNotFound() {
        Policy policy = new Policy();
        policy.setId(policyId);
        policy.setInsurerId(insurerId);
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(insurerRepository.findById(insurerId)).thenReturn(Optional.empty());

        VisitorCreatedEvent event = new VisitorCreatedEvent(visitorId, policyId);
        assertThatThrownBy(() -> listener.onVisitorCreated(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insurer not found");
        verify(insurerRepository, never()).consumeOnePolicyToken(any());
    }

    @Test
    void onVisitorCreatedSyncsTheTokenReadBackFromTheDatabaseToTheOrganization() {
        UUID organizationId = UUID.randomUUID();
        // Stale entity value (1000) deliberately differs from the DB value after concurrent decrements.
        givenPolicyAndInsurer(insurerWithToken(1000L, organizationId));
        when(insurerRepository.consumeOnePolicyToken(insurerId)).thenReturn(1);
        when(insurerRepository.findPolicyTokenById(insurerId)).thenReturn(Optional.of(997L));

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(organizationService).syncPolicyToken(organizationId, 997L);
    }

    @Test
    void onVisitorCreatedSkipsOrganizationSyncWhenInsurerHasNoOrganization() {
        givenPolicyAndInsurer(insurerWithToken(10L, null));
        when(insurerRepository.consumeOnePolicyToken(insurerId)).thenReturn(1);
        when(insurerRepository.findPolicyTokenById(insurerId)).thenReturn(Optional.of(9L));

        listener.onVisitorCreated(new VisitorCreatedEvent(visitorId, policyId));

        verify(organizationService, never()).syncPolicyToken(any(), any());
    }
}
