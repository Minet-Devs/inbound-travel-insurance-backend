package com.travel.insurance.document;

/** Raised when the blob store cannot be read or written; mapped to 503. */
public class DocumentStorageException extends RuntimeException {

    public DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
