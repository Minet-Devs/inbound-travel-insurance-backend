package com.travel.insurance.biometric;

import com.travel.insurance.biometric.dto.BiometricCallbackPayload;
import com.travel.insurance.config.EkYcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class SecureHashVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final EkYcProperties properties;

    public boolean isValid(BiometricCallbackPayload payload) {
        String mac = HexFormat.of().formatHex(
                hmacSha256(properties.getClientSecret(), concat(payload.result(), payload.requestId(), properties.getClientId(), payload.relyingPartyRequestId())))
                .toLowerCase(Locale.ROOT);
        String expected = java.util.Base64.getEncoder().encodeToString(mac.getBytes(StandardCharsets.UTF_8));
        byte[] given = (payload.secureHash() == null ? "" : payload.secureHash()).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given);
    }

    private static byte[] hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA256 is not available", ex);
        }
    }

    private static String concat(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part == null ? "" : part);
        }
        return builder.toString();
    }
}
