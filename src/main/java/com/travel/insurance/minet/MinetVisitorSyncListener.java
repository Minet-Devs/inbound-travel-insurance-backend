package com.travel.insurance.minet;

import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorCreatedEvent;
import com.travel.insurance.visitor.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.Arrays;

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
        String[] names = visitor.getFullName().trim().split("\\s+");
        String firstName = names[0];
        String lastName = names.length > 1 ? names[names.length - 1] : "";
        String otherNames = names.length > 2
                ? String.join(" ", Arrays.copyOfRange(names, 1, names.length - 1))
                : null;
        LocalDate dateJoined = visitor.getDateIn() != null ? visitor.getDateIn() : LocalDate.now();
        return new MinetVisitorRequest(
                visitor.getPassportNumber(),
                firstName,
                lastName,
                otherNames,
                visitor.getGender().name(),
                visitor.getDateOfBirth(),
                dateJoined,
                visitor.getEmail(),
                visitor.getPhoneNumber(),
                visitor.getNationality());
    }
}
