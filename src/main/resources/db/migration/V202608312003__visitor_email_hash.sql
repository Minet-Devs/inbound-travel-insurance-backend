-- Adds a deterministic HMAC blind-index column for visitor email lookups
-- (mobile OTP login), since visitors.email is stored as randomized AES-GCM
-- ciphertext and can't be searched by value directly. Nullable and
-- unindexed-for-uniqueness on purpose, same tradeoff as passport_number_hash
-- in V202608171753__visitor_encryption_columns.sql: pre-existing rows need
-- EncryptionBackfillRunner (see common/crypto) to populate it, and unlike
-- passport number, email is not treated as unique per visitor (see
-- backend-architecture.md "Mobile Visitor Login").

alter table visitors add column email_hash varchar(64);

create index idx_visitors_email_hash on visitors (email_hash);
