package com.travel.insurance.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class BlindIndexServiceTest {

    private BlindIndexService blindIndexService;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        EncryptionKeyProvider keyProvider = new EncryptionKeyProvider() {
            @Override
            public javax.crypto.SecretKey getDataKey() {
                throw new UnsupportedOperationException();
            }

            @Override
            public javax.crypto.SecretKey getBlindIndexKey() {
                return new SecretKeySpec(keyBytes, "HmacSHA256");
            }
        };
        blindIndexService = new BlindIndexService(keyProvider);
    }

    @Test
    void hmacIsDeterministicForSameInput() {
        assertThat(blindIndexService.hmac("P1234567")).isEqualTo(blindIndexService.hmac("P1234567"));
    }

    @Test
    void hmacDiffersForDifferentInput() {
        assertThat(blindIndexService.hmac("P1234567")).isNotEqualTo(blindIndexService.hmac("P7654321"));
    }

    @Test
    void hmacNormalizesCaseAndWhitespaceLikeOldIgnoreCaseLookup() {
        assertThat(blindIndexService.hmac("p1234567")).isEqualTo(blindIndexService.hmac("P1234567"));
        assertThat(blindIndexService.hmac(" P1234567 ")).isEqualTo(blindIndexService.hmac("P1234567"));
    }

    @Test
    void hmacReturnsNullForNullInput() {
        assertThat(blindIndexService.hmac(null)).isNull();
    }
}
