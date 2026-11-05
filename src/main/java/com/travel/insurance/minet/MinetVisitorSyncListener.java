package com.travel.insurance.minet;

import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import com.travel.insurance.visitor.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pushes every newly created visitor to the Minet system once the creating
 * transaction has committed. A Minet failure is logged and never affects the
 * visitor that was already saved.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinetVisitorSyncListener {

    private final VisitorService visitorService;
    private final MinetClient minetClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVisitorCreated(VisitorCreatedEvent event) {
        try {
            Visitor visitor = visitorService.getEntityById(event.visitorId());
            if (visitor.getCertificateSerialNumber() == null) {
                log.warn("Visitor {} has no certificate serial number yet; not sending to Minet", event.visitorId());
                return;
            }
            minetClient.sendVisitor(toRequest(visitor));
            visitorService.markSentToMinet(event.visitorId());
        } catch (Exception ex) {
            log.error("Failed to send visitor {} to Minet: {}", event.visitorId(), ex.getMessage(), ex);
        }
    }

    static MinetVisitorRequest toRequest(Visitor visitor) {
        return MinetVisitorRequest.of(visitor.getPassportNumber(), visitor.getFullName(), visitor.getGender(),
                visitor.getDateOfBirth(), visitor.getDateIn(), visitor.getEmail(), visitor.getPhoneNumber(),
                visitor.getNationality());
    }
}
