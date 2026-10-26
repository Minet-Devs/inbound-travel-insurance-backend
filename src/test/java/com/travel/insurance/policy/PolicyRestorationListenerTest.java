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

import static org.assertj.core.api.Assertions.assertThat;
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

    private Insurer stubInsurer(Long token, UUID orgId) {
        Policy policy = new Policy();
        policy.setId(policyId);
        policy.setInsurerId(insurerId);

        Insurer insurer = new Insurer();
        insurer.setId(insurerId);
        insurer.setName("Minet Insurance");
        insurer.setPolicyToken(token);
        insurer.setOrganizationId(orgId);

        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(insurerRepository.findById(insurerId)).thenReturn(Optional.of(insurer));
        when(insurerRepository.save(any(Insurer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return insurer;
    }

    @Test
    void onVisitorDeletedIncrementsTokenAndSyncsOrganization() {
        Insurer insurer = stubInsurer(999L, organizationId);

        listener.onVisitorDeleted(new VisitorDeletedEvent(UUID.randomUUID(), policyId));

        assertThat(insurer.getPolicyToken()).isEqualTo(1000L);
        verify(organizationService).syncPolicyToken(organizationId, 1000L);
    }

    @Test
    void onVisitorDeletedInitializesNullTokenAndSyncsOrganization() {
        Insurer insurer = stubInsurer(null, organizationId);

        listener.onVisitorDeleted(new VisitorDeletedEvent(UUID.randomUUID(), policyId));

        assertThat(insurer.getPolicyToken()).isEqualTo(1L);
        verify(organizationService).syncPolicyToken(organizationId, 1L);
    }

    @Test
    void onVisitorDeletedSkipsOrganizationSyncWhenInsurerHasNoOrganization() {
        stubInsurer(5L, null);

        listener.onVisitorDeleted(new VisitorDeletedEvent(UUID.randomUUID(), policyId));

        verify(organizationService, never()).syncPolicyToken(any(), any());
    }
}
