package com.travel.insurance.minet;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.visitor.Gender;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinetVisitorSyncServiceImplTest {

    @Mock
    private VisitorService visitorService;

    @Mock
    private MinetClient minetClient;

    @InjectMocks
    private MinetVisitorSyncServiceImpl service;

    private final UUID visitorId = UUID.randomUUID();

    private Visitor visitor(String serial, boolean alreadySent) {
        Visitor visitor = new Visitor();
        visitor.setId(visitorId);
        visitor.setFullName("GARIMA THAPLIYAL");
        visitor.setCertificateSerialNumber(serial);
        visitor.setPassportNumber("A1234567");
        visitor.setGender(Gender.FEMALE);
        visitor.setDateOfBirth(LocalDate.of(1994, 7, 1));
        visitor.setDateIn(LocalDate.of(2026, 11, 6));
        visitor.setNationality("IND");
        visitor.setSentToMinet(alreadySent);
        return visitor;
    }

    @Test
    void sendsVisitorThenMarksItSent() {
        when(visitorService.getEntityByPassportNumber("A1234567")).thenReturn(visitor("SP1", false));

        MinetSyncResponse response = service.syncByPassportNumber("A1234567");

        verify(minetClient).sendVisitor(MinetVisitorSyncListener.toRequest(visitor("SP1", false)));
        verify(visitorService).markSentToMinet(visitorId);
        assertThat(response).isEqualTo(new MinetSyncResponse(visitorId, true));
    }

    @Test
    void sendsAgainWhenAlreadySent() {
        when(visitorService.getEntityByPassportNumber("A1234567")).thenReturn(visitor("SP1", true));

        MinetSyncResponse response = service.syncByPassportNumber("A1234567");

        verify(minetClient).sendVisitor(any());
        verify(visitorService).markSentToMinet(visitorId);
        assertThat(response.sentToMinet()).isTrue();
    }

    @Test
    void unknownPassportPropagatesNotFound() {
        when(visitorService.getEntityByPassportNumber("X")).thenThrow(new ResourceNotFoundException("Visitor", "X"));

        assertThatThrownBy(() -> service.syncByPassportNumber("X")).isInstanceOf(ResourceNotFoundException.class);
        verify(minetClient, never()).sendVisitor(any());
    }

    @Test
    void visitorWithoutCertificateSerialIsRejected() {
        when(visitorService.getEntityByPassportNumber("A1234567")).thenReturn(visitor(null, false));

        assertThatThrownBy(() -> service.syncByPassportNumber("A1234567"))
                .isInstanceOf(IllegalStateException.class);
        verify(minetClient, never()).sendVisitor(any());
        verify(visitorService, never()).markSentToMinet(any());
    }

    @Test
    void minetFailureLeavesFlagUntouched() {
        when(visitorService.getEntityByPassportNumber("A1234567")).thenReturn(visitor("SP1", false));
        doThrow(new MinetClientException("boom")).when(minetClient).sendVisitor(any());

        assertThatThrownBy(() -> service.syncByPassportNumber("A1234567"))
                .isInstanceOf(MinetClientException.class);
        verify(visitorService, never()).markSentToMinet(any());
    }
}
