package com.travel.insurance.document.storage;

import java.io.InputStream;

/** Binary store for merged document PDFs, addressed by an opaque storage key. */
public interface DocumentStorage {

    void put(String key, InputStream content, long length, String contentType);

    InputStream get(String key);

    void delete(String key);
}
