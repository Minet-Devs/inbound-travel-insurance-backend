package com.travel.insurance.common.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, UUID id) {
        super("%s not found: %s".formatted(resource, id));
    }

    public ResourceNotFoundException(String resource, String passportNumber) {
        super("%s not found: %s".formatted(resource, passportNumber));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
