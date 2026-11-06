package com.travel.insurance.policy;

import com.travel.insurance.insurer.Insurer;
import com.travel.insurance.insurer.InsurerRepository;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.visitor.VisitorDeletedEvent;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyRestorationListenerTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private InsurerRepository insurerRepository;

    @Mock
    private OrganizationService organizationService;

    private PolicyRestorationListener listener;

    private final UUID policyId = UUID.randomUUID();
    private final UUID insurerId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new PolicyRestorationListener(policyRepository, insurerRepository, organizationService);
    }

    private void stubInsurer(UUID orgId, long tokenAfterRestore) {
        Policy policy = new Policy();
        policy.setId(policyId);
        policy.setInsurerId(insurerId);

        Insurer insurer = new Insurer();
        insurer.setId(insurerId);
        insurer.setName("Minet Insurance");
        // Deliberately stale: the listener must not trust the entity's token.
        insurer.setPolicyToken(0L);
        insurer.setOrganizationId(orgId);

        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(insurerRepository.findById(insurerId)).thenReturn(Optional.of(insurer));
        when(insurerRepository.restoreOnePolicyToken(insurerId)).thenReturn(1);
        when(insurerRepository.findPolicyTokenById(insurerId)).thenReturn(Optional.of(tokenAfterRestore));
    }

    @Test
    void onVisitorDeletedRestoresTokenAtomicallyAndSyncsTheValueReadBackToTheOrganization() {
        stubInsurer(organizationId, 1000L);

        listener.onVisitorDeleted(new VisitorDeletedEvent(UUID.randomUUID(), policyId));

        verify(insurerRepository).restoreOnePolicyToken(insurerId);
        verify(insurerRepository, never()).save(any(Insurer.class));
        verify(organizationService).syncPolicyToken(organizationId, 1000L);
    }

    @Test
    void onVisitorDeletedSyncsOneWhenTheQuotaWasNull() {
        stubInsurer(organizationId, 1L);

        listener.onVisitorDeleted(new VisitorDeletedEvent(UUID.randomUUID(), policyId));

        verify(organizationService).syncPolicyToken(organizationId, 1L);
    }

    @Test
    void onVisitorDeletedSkipsOrganizationSyncWhenInsurerHasNoOrganization() {
        stubInsurer(null, 6L);

        listener.onVisitorDeleted(new VisitorDeletedEvent(UUID.randomUUID(), policyId));

        verify(organizationService, never()).syncPolicyToken(any(), any());
    }

    @Test
    void onVisitorDeletedThrowsWhenPolicyNotFound() {
        when(policyRepository.findById(policyId)).thenReturn(Optional.empty());

        VisitorDeletedEvent event = new VisitorDeletedEvent(UUID.randomUUID(), policyId);
        assertThatThrownBy(() -> listener.onVisitorDeleted(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Policy not found");
        verify(insurerRepository, never()).restoreOnePolicyToken(any());
    }

    @Test
    void onVisitorDeletedThrowsWhenInsurerNotFound() {
        Policy policy = new Policy();
        policy.setId(policyId);
        policy.setInsurerId(insurerId);
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(insurerRepository.findById(insurerId)).thenReturn(Optional.empty());

        VisitorDeletedEvent event = new VisitorDeletedEvent(UUID.randomUUID(), policyId);
        assertThatThrownBy(() -> listener.onVisitorDeleted(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insurer not found");
        verify(insurerRepository, never()).restoreOnePolicyToken(any());
    }
}
