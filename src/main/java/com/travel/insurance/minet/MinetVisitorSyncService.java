package com.travel.insurance.minet;

import com.travel.insurance.visitor.VisitorStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MinetVisitorSyncService {

    /**
     * Sends the visitor with the given passport number to Minet and, only once Minet accepted it,
     * sets {@code sent_to_minet = true}. Sends again if the flag was already set. Throws
     * {@code ResourceNotFoundException} for an unknown passport, {@code IllegalStateException} when the
     * visitor has no certificate serial number yet, and {@link MinetClientException} when Minet fails.
     */
    MinetSyncResponse syncByPassportNumber(String passportNumber);

    /**
     * The member (visitor) with this certificate serial number, in the Minet member shape. Throws
     * {@code ResourceNotFoundException} for an unknown certificate number.
     */
    MinetVisitorRequest getMemberByCertificateNumber(String certificateNumber);

    /**
     * Visitors in the Minet payload shape, one per visitor, for the Excel export. Filters are optional;
     * {@code dateFrom}/{@code dateTo} are inclusive and match the visitor's entry date.
     */
    List<MinetVisitorRequest> listForExport(UUID insurerId, VisitorStatus status,
                                            LocalDate dateFrom, LocalDate dateTo);
}
