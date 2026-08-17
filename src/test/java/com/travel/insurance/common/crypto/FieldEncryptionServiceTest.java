package com.travel.insurance.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldEncryptionServiceTest {

    private FieldEncryptionService fieldEncryptionService;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        EncryptionKeyProvider keyProvider = new EncryptionKeyProvider() {
            @Override
            public javax.crypto.SecretKey getDataKey() {
                return new SecretKeySpec(keyBytes, "AES");
            }

            @Override
            public javax.crypto.SecretKey getBlindIndexKey() {
                throw new UnsupportedOperationException();
            }
        };
        fieldEncryptionService = new FieldEncryptionService(keyProvider);
    }

    @Test
    void encryptThenDecryptRoundTripsToOriginalValue() {
        String plaintext = "P1234567";

        String ciphertext = fieldEncryptionService.encrypt(plaintext);

        assertThat(ciphertext).isNotEqualTo(plaintext);
        assertThat(fieldEncryptionService.decrypt(ciphertext)).isEqualTo(plaintext);
    }

    @Test
    void encryptProducesDifferentCiphertextForSamePlaintext() {
        String plaintext = "jane.traveler@example.com";

        String first = fieldEncryptionService.encrypt(plaintext);
        String second = fieldEncryptionService.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void nullAndEmptyPassThroughUnchanged() {
        assertThat(fieldEncryptionService.encrypt(null)).isNull();
        assertThat(fieldEncryptionService.decrypt(null)).isNull();
        assertThat(fieldEncryptionService.encrypt("")).isEmpty();
        assertThat(fieldEncryptionService.decrypt("")).isEmpty();
    }

    @Test
    void decryptFailsWhenCiphertextIsTampered() {
        String ciphertext = fieldEncryptionService.encrypt("sensitive-value");
        byte[] raw = Base64.getDecoder().decode(ciphertext);
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> fieldEncryptionService.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isEncryptedRecognizesOwnCiphertext() {
        String ciphertext = fieldEncryptionService.encrypt("P1234567");

        assertThat(fieldEncryptionService.isEncrypted(ciphertext)).isTrue();
    }

    @Test
    void isEncryptedReturnsFalseForPlaintext() {
        assertThat(fieldEncryptionService.isEncrypted("P1234567")).isFalse();
        assertThat(fieldEncryptionService.isEncrypted("jane.traveler@example.com")).isFalse();
        assertThat(fieldEncryptionService.isEncrypted("not-base64-!!!")).isFalse();
    }

    @Test
    void isEncryptedReturnsTrueForNullOrEmpty() {
        assertThat(fieldEncryptionService.isEncrypted(null)).isTrue();
        assertThat(fieldEncryptionService.isEncrypted("")).isTrue();
    }
}
