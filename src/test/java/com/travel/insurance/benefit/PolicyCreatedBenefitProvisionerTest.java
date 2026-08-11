package com.travel.insurance.benefit;

import com.travel.insurance.policy.PolicyCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PolicyCreatedBenefitProvisionerTest {

    @Mock
    private BenefitService benefitService;

    @InjectMocks
    private PolicyCreatedBenefitProvisioner provisioner;

    @Test
    void provisionsFixedBenefitsForTheNewPolicy() {
        UUID policyId = UUID.randomUUID();

        provisioner.onPolicyCreated(new PolicyCreatedEvent(policyId));

        verify(benefitService).provisionFixedBenefits(policyId);
    }
}
