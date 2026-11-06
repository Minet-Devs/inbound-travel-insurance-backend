package com.travel.insurance.minet;

import com.travel.insurance.claim.PreauthorizationConvertedToClaimEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MinetClaimSyncListenerTest {

    @Mock
    private MinetClaimSyncService minetClaimSyncService;

    @InjectMocks
    private MinetClaimSyncListener listener;

    private final UUID claimId = UUID.randomUUID();
    private final PreauthorizationConvertedToClaimEvent event =
            new PreauthorizationConvertedToClaimEvent(claimId, UUID.randomUUID());

    @Test
    void sendsTheConvertedClaim() {
        listener.onConverted(event);

        verify(minetClaimSyncService).syncClaim(claimId);
    }

    @Test
    void missingMinetProviderIdIsSwallowed() {
        doThrow(new IllegalStateException("no minet id")).when(minetClaimSyncService).syncClaim(claimId);

        assertThatCode(() -> listener.onConverted(event)).doesNotThrowAnyException();
    }

    @Test
    void minetFailureIsSwallowed() {
        doThrow(new MinetClientException("boom")).when(minetClaimSyncService).syncClaim(claimId);

        assertThatCode(() -> listener.onConverted(event)).doesNotThrowAnyException();
    }
}
