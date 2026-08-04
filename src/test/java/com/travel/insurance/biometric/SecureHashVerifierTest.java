package com.travel.insurance.biometric;

import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.config.EkYcProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureHashVerifierTest {

    private static final String CLIENT_SECRET = "YXbpnafpVZP6B75ehYN2WhOtqST1w4b8";

    private SecureHashVerifier verifier;

    @BeforeEach
    void setUp() {
        EkYcProperties properties = new EkYcProperties();
        properties.setClientSecret(CLIENT_SECRET);
        verifier = new SecureHashVerifier(properties);
    }

    @Test
    void acceptsCallbackWithValidSecureHash() {
        BiometricCallbackPayload payload = new BiometricCallbackPayload(
                "529fd955-0000-0000-0000-000000000001",
                "prism-verification-uuid",
                "accepted",
                "match",
                "DICP2000",
                3,
                "NTY4OTE5ODMwMzI0YWY1OGMwYWNmNzc1MDFiYTE1MDBmMGY4MzZmNTM4NDA3MTVhNDA0YzY4OTE3ODM5ODg2NA==");

        assertThat(verifier.isValid(payload)).isTrue();
    }

    @Test
    void rejectsCallbackWithTamperedSecureHash() {
        BiometricCallbackPayload payload = new BiometricCallbackPayload(
                "529fd955-0000-0000-0000-000000000001",
                "prism-verification-uuid",
                "accepted",
                "match",
                "DICP2000",
                3,
                "AAAAAAAA");

        assertThat(verifier.isValid(payload)).isFalse();
    }

    @Test
    void rejectsCallbackWithMissingSecureHash() {
        BiometricCallbackPayload payload = new BiometricCallbackPayload(
                "529fd955-0000-0000-0000-000000000001",
                "prism-verification-uuid",
                "accepted",
                "match",
                "DICP2000",
                3,
                null);

        assertThat(verifier.isValid(payload)).isFalse();
    }
}
