package com.travel.insurance.minet;

import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MinetVisitorSyncServiceImpl implements MinetVisitorSyncService {

    private final VisitorService visitorService;
    private final MinetClient minetClient;

    @Override
    public MinetSyncResponse syncByPassportNumber(String passportNumber) {
        Visitor visitor = visitorService.getEntityByPassportNumber(passportNumber);
        if (visitor.getCertificateSerialNumber() == null) {
            throw new IllegalStateException("Visitor has no certificate serial number yet; cannot send to Minet");
        }
        minetClient.sendVisitor(MinetVisitorSyncListener.toRequest(visitor));
        visitorService.markSentToMinet(visitor.getId());
        return new MinetSyncResponse(visitor.getId(), true);
    }
}
