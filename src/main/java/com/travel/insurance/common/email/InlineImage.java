package com.travel.insurance.common.email;

/**
 * An image embedded in the HTML body and referenced as {@code <img src="cid:contentId">}.
 * Deliberately generic (no domain knowledge) so the {@link EmailService} stays reusable.
 */
public record InlineImage(String contentId, String contentType, byte[] content) {
}
