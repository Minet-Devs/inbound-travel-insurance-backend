package com.travel.insurance.minet;

import com.travel.insurance.visitor.Gender;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import com.travel.insurance.visitor.VisitorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinetVisitorSyncListenerTest {

    @Mock
    private VisitorService visitorService;

    @Mock
    private MinetClient minetClient;

    @InjectMocks
    private MinetVisitorSyncListener listener;

    private final UUID visitorId = UUID.randomUUID();
    private final VisitorCreatedEvent event = new VisitorCreatedEvent(visitorId, UUID.randomUUID());

    private Visitor visitor(String fullName, String serial) {
        Visitor visitor = new Visitor();
        visitor.setFullName(fullName);
        visitor.setCertificateSerialNumber(serial);
        visitor.setPassportNumber("A1234567");
        visitor.setGender(Gender.FEMALE);
        visitor.setDateOfBirth(LocalDate.of(1994, 7, 1));
        visitor.setDateIn(LocalDate.of(2026, 11, 6));
        visitor.setEmail("mbashiavictor@gmail.com");
        visitor.setPhoneNumber("+254791531926");
        visitor.setNationality("IND");
        return visitor;
    }

    @Test
    void sendsMappedVisitorToMinet() {
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor("GARIMA THAPLIYAL", "SP003369"));

        listener.onVisitorCreated(event);

        ArgumentCaptor<MinetVisitorRequest> captor = ArgumentCaptor.forClass(MinetVisitorRequest.class);
        verify(minetClient).sendVisitor(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new MinetVisitorRequest(
                "A1234567", "GARIMA", "THAPLIYAL", null, "FEMALE",
                LocalDate.of(1994, 7, 1), LocalDate.of(2026, 11, 6),
                "mbashiavictor@gmail.com", "+254791531926", "IND"));
        verify(visitorService).markSentToMinet(visitorId);
    }

    @Test
    void middleNamesGoToOtherNames() {
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor("Anna Maria Louise Smith", "SP1"));

        listener.onVisitorCreated(event);

        ArgumentCaptor<MinetVisitorRequest> captor = ArgumentCaptor.forClass(MinetVisitorRequest.class);
        verify(minetClient).sendVisitor(captor.capture());
        assertThat(captor.getValue().firstName()).isEqualTo("Anna");
        assertThat(captor.getValue().otherNames()).isEqualTo("Maria Louise");
        assertThat(captor.getValue().lastName()).isEqualTo("Smith");
    }

    @Test
    void skipsVisitorWithoutCertificateSerialNumber() {
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor("GARIMA THAPLIYAL", null));

        listener.onVisitorCreated(event);

        verify(minetClient, never()).sendVisitor(any());
        verify(visitorService, never()).markSentToMinet(any());
    }

    @Test
    void minetFailureIsSwallowed() {
        when(visitorService.getEntityById(visitorId)).thenReturn(visitor("GARIMA THAPLIYAL", "SP003369"));
        doThrow(new MinetClientException("boom")).when(minetClient).sendVisitor(any());

        assertThatCode(() -> listener.onVisitorCreated(event)).doesNotThrowAnyException();
        verify(visitorService, never()).markSentToMinet(any());
    }
}
