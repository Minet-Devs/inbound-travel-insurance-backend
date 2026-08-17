package com.travel.insurance.common.crypto;

import javax.crypto.SecretKey;

/**
 * Seam for sourcing the encryption keys used for field-level encryption.
 * The default implementation reads keys from env-backed config; swap this
 * out for a KMS/Vault-backed implementation without touching converters.
 */
public interface EncryptionKeyProvider {

    SecretKey getDataKey();

    SecretKey getBlindIndexKey();
}
