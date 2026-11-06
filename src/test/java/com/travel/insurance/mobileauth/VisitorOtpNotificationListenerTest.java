package com.travel.insurance.mobileauth;

import com.travel.insurance.config.AsyncConfig;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorOtpNotificationListenerTest {

    @Test
    void afterCommitListenersRunOnTheNotificationExecutor() {
        assertThat(Arrays.stream(VisitorOtpNotificationListener.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(TransactionalEventListener.class)))
                .isNotEmpty()
                .allSatisfy(m -> assertThat(m.getAnnotation(Async.class))
                        .isNotNull()
                        .extracting(Async::value).isEqualTo(AsyncConfig.NOTIFICATION_EXECUTOR));
    }
}
