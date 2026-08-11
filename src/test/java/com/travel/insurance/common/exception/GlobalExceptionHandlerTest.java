package com.travel.insurance.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRequestURI(uri);
        return req;
    }

    @Test
    void unexpectedErrorIsWrittenAsJsonRegardlessOfNegotiatedType() {
        // Reproduces the "No converter for ApiError with preset Content-Type 'text/javascript'"
        // failure: the error body must pin application/json so Jackson always applies.
        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new RuntimeException("boom"), request("/widget.js"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().path()).isEqualTo("/widget.js");
    }

    @Test
    void notFoundIsWrittenAsJson() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("nope"), request("/api/v1/x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }
}
