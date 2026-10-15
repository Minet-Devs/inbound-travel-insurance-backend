package com.travel.insurance.document;

/** Raised when an upload exceeds the configured size limits; mapped to 413. */
public class DocumentTooLargeException extends RuntimeException {

    public DocumentTooLargeException(String message) {
        super(message);
    }
}
