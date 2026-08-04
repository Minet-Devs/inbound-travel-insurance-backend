package com.travel.insurance.biometric.client;

public class EkYcClientException extends RuntimeException {

    public EkYcClientException(String message) {
        super(message);
    }

    public EkYcClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
