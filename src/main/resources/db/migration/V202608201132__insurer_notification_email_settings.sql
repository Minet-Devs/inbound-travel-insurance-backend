-- Add per-insurer outbound-email/e-signature settings for policy document
-- notifications. notification_email_password is stored as text to
-- accommodate AES-256-GCM ciphertext (see common/crypto), matching the
-- pattern used for other credential/PII columns.

alter table insurers add column notification_email varchar(255);
alter table insurers add column notification_email_password text;
alter table insurers add column host varchar(255);
alter table insurers add column port integer;
alter table insurers add column esignature text;
