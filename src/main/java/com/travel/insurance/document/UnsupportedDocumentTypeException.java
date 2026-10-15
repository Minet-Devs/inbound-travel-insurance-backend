package com.travel.insurance.document;

/** Raised when an uploaded file is not a PDF; mapped to 415. */
public class UnsupportedDocumentTypeException extends RuntimeException {

    public UnsupportedDocumentTypeException(String message) {
        super(message);
    }
}
