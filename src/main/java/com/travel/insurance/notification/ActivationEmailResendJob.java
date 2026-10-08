package com.travel.insurance.notification;

import com.travel.insurance.config.MailProperties;
import com.travel.insurance.visitor.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Safety net for the activation email: picks up ACTIVE visitors whose email was never
 * delivered (SMTP outage, incomplete benefit schedule at activation time) and hands each
 * back to {@link VisitorActivatedNotificationListener}, which re-checks the visitor is
 * still pending before sending. Only visitors created between {@code maxAge} and
 * {@code minAge} ago are considered. With several backend instances two runs could pick
 * the same visitor; the pending re-check narrows but does not eliminate that window.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "app.mail.resend", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ActivationEmailResendJob {

    private final VisitorService visitorService;
    private final VisitorActivatedNotificationListener listener;
    private final MailProperties mailProperties;

    @Scheduled(fixedDelayString = "${app.mail.resend.interval:PT10M}",
            initialDelayString = "${app.mail.resend.interval:PT10M}")
    public void run() {
        MailProperties.Resend cfg = mailProperties.getResend();
        Instant now = Instant.now();
        List<UUID> pending = visitorService.findIdsAwaitingActivationEmail(
                now.minus(cfg.getMaxAge()), now.minus(cfg.getMinAge()), cfg.getBatchSize());
        if (pending.isEmpty()) {
            return;
        }
        log.info("Re-sending activation email for {} visitor(s) that never received it", pending.size());
        for (UUID visitorId : pending) {
            try {
                listener.resendActivationEmailIfPending(visitorId);
            } catch (Exception ex) {
                log.error("Activation email re-send failed for visitor {}: {}", visitorId, ex.getMessage(), ex);
            }
        }
    }
}
