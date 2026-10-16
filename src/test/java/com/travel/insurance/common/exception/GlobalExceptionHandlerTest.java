package com.travel.insurance.common.exception;

import com.travel.insurance.document.DocumentStorageException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;

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
    void typeMismatchIsReportedAsBadRequestNotServerError() throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyEndpoint", LocalDate.class), 0);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "01-08-2026", LocalDate.class, "fromDate", parameter, new IllegalArgumentException("bad date"));

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex, request("/api/v1/member-statements/export"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid value for parameter 'fromDate': expected LocalDate");
    }

    @SuppressWarnings("unused")
    private void dummyEndpoint(LocalDate fromDate) {
    }

    @Test
    void notFoundIsWrittenAsJson() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("nope"), request("/api/v1/x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void storageFailureIsReportedAsServiceUnavailableWithoutLeakingDetails() {
        ResponseEntity<ApiError> response = handler.handleDocumentStorage(
                new DocumentStorageException("Could not store document x", new RuntimeException("conn string")),
                request("/api/v1/claims/1/documents"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().message()).isEqualTo("Document storage is temporarily unavailable");
    }

    @Test
    void malformedMultipartIsReportedAsBadRequest() {
        ResponseEntity<ApiError> response = handler.handleMalformedMultipart(
                new MultipartException("Failed to parse multipart servlet request"),
                request("/api/v1/claims/1/documents"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("Failed to parse multipart servlet request");
    }
}
