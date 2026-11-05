package com.travel.insurance.minet;

public interface MinetVisitorSyncService {

    /**
     * Sends the visitor with the given passport number to Minet and, only once Minet accepted it,
     * sets {@code sent_to_minet = true}. Sends again if the flag was already set. Throws
     * {@code ResourceNotFoundException} for an unknown passport, {@code IllegalStateException} when the
     * visitor has no certificate serial number yet, and {@link MinetClientException} when Minet fails.
     */
    MinetSyncResponse syncByPassportNumber(String passportNumber);
}
