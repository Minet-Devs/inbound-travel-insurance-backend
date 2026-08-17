package com.travel.insurance.common.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Deterministic HMAC-SHA256 "blind index" for equality lookups on
 * encrypted columns (e.g. passport number), since randomized AES-GCM
 * ciphertext cannot be searched or uniquely constrained directly.
 */
@Component
@RequiredArgsConstructor
public class BlindIndexService {

    private static final String ALGORITHM = "HmacSHA256";

    private final EncryptionKeyProvider keyProvider;

    public String hmac(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(keyProvider.getBlindIndexKey().getEncoded(), ALGORITHM));
            byte[] digest = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute blind index", e);
        }
    }
}
