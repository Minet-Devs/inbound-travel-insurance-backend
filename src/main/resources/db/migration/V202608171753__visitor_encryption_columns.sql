-- Widen visitor PII/medical columns to text to accommodate AES-256-GCM
-- ciphertext (see common/crypto), and add a deterministic HMAC blind index
-- column for passport-number lookups/uniqueness, since randomized
-- ciphertext can no longer be searched or uniquely constrained directly.
--
-- passport_number_hash is added NULLABLE and un-indexed here on purpose:
-- environments with pre-existing visitor rows have plaintext data that
-- must be backfilled (encrypted + hashed) by EncryptionBackfillRunner
-- (see common/crypto) before the NOT NULL constraint and unique index can
-- be added safely. See backend-architecture.md "Security" for the
-- deployment runbook. A follow-up migration adds the NOT NULL + unique
-- index once backfill is confirmed complete.

alter table visitors
    alter column full_name type text,
    alter column passport_number type text,
    alter column date_of_birth type text,
    alter column nationality type text,
    alter column address type text,
    alter column email type text,
    alter column phone_number type text,
    alter column underlying_conditions type text,
    alter column next_of_kin_name type text,
    alter column next_of_kin_phone type text;

drop index if exists uq_visitors_passport_number;

alter table visitors add column passport_number_hash varchar(64);
