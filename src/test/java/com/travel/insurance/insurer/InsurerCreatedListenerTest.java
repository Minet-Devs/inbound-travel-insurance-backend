package com.travel.insurance.insurer;

import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.policy.dto.PolicyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InsurerCreatedListenerTest {

    @Mock
    private PolicyService policyService;

    @Test
    void onInsurerCreatedCreatesPolicyForThatInsurer() {
        InsurerCreatedListener listener = new InsurerCreatedListener(policyService);
        UUID insurerId = UUID.randomUUID();

        listener.onInsurerCreated(new InsurerCreatedEvent(insurerId));

        ArgumentCaptor<PolicyRequest> captor = ArgumentCaptor.forClass(PolicyRequest.class);
        verify(policyService).create(captor.capture());
        assertThat(captor.getValue().insurerId()).isEqualTo(insurerId);
        assertThat(captor.getValue().status()).isNull();
    }
}
