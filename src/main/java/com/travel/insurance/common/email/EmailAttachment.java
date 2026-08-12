package com.travel.insurance.common.email;

/**
 * A single email attachment: its filename as shown to the recipient and the
 * raw bytes of the file. Deliberately generic (no domain knowledge) so the
 * {@link EmailService} stays reusable across features.
 */
public record EmailAttachment(String filename, byte[] content) {
}
