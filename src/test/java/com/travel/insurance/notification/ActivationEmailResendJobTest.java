package com.travel.insurance.notification;

import com.travel.insurance.config.MailProperties;
import com.travel.insurance.visitor.VisitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivationEmailResendJobTest {

    @Mock
    private VisitorService visitorService;
    @Mock
    private VisitorActivatedNotificationListener listener;

    private ActivationEmailResendJob job;

    @BeforeEach
    void setUp() {
        job = new ActivationEmailResendJob(visitorService, listener, new MailProperties());
    }

    @Test
    void resendsEveryPendingVisitorWithinTheConfiguredWindow() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(visitorService.findIdsAwaitingActivationEmail(any(), any(), eq(50))).thenReturn(List.of(a, b));

        job.run();

        verify(listener).resendActivationEmailIfPending(a);
        verify(listener).resendActivationEmailIfPending(b);
        ArgumentCaptor<Instant> after = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> before = ArgumentCaptor.forClass(Instant.class);
        verify(visitorService).findIdsAwaitingActivationEmail(after.capture(), before.capture(), eq(50));
        assertThat(Duration.between(after.getValue(), before.getValue()))
                .isBetween(Duration.ofDays(7).minusMinutes(5).minusSeconds(5),
                        Duration.ofDays(7).minusMinutes(5).plusSeconds(5));
    }

    @Test
    void doesNothingWhenNoVisitorIsPending() {
        when(visitorService.findIdsAwaitingActivationEmail(any(), any(), eq(50))).thenReturn(List.of());

        job.run();

        verifyNoInteractions(listener);
    }

    @Test
    void oneFailingVisitorDoesNotStopTheRest() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(visitorService.findIdsAwaitingActivationEmail(any(), any(), eq(50))).thenReturn(List.of(a, b));
        doThrow(new RuntimeException("boom")).when(listener).resendActivationEmailIfPending(a);

        job.run();

        verify(listener).resendActivationEmailIfPending(b);
    }
}
