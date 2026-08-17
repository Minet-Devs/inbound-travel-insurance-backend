-- Widen biometric verification columns to text to accommodate AES-256-GCM
-- ciphertext (see common/crypto).

alter table biometric_verifications
    alter column subject_id_number type text,
    alter column embeded_token type text;
