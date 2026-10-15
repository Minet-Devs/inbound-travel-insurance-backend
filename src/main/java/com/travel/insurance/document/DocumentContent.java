package com.travel.insurance.document;

import com.travel.insurance.document.dto.DocumentResponse;

import java.io.InputStream;

/** A document's metadata plus its byte stream; the caller must close the stream. */
public record DocumentContent(DocumentResponse metadata, InputStream stream) {
}
