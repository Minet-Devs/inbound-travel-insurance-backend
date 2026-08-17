package com.travel.insurance.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EnvEncryptionKeyProvider implements EncryptionKeyProvider {

    private final SecretKey dataKey;
    private final SecretKey blindIndexKey;

    public EnvEncryptionKeyProvider(@Value("${app.encryption.data-key}") String dataKeyBase64,
                                    @Value("${app.encryption.blind-index-key}") String blindIndexKeyBase64) {
        this.dataKey = new SecretKeySpec(Base64.getDecoder().decode(dataKeyBase64), "AES");
        this.blindIndexKey = new SecretKeySpec(Base64.getDecoder().decode(blindIndexKeyBase64), "HmacSHA256");
    }

    @Override
    public SecretKey getDataKey() {
        return dataKey;
    }

    @Override
    public SecretKey getBlindIndexKey() {
        return blindIndexKey;
    }
}
