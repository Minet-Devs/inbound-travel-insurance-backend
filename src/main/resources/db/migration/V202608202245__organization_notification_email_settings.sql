-- Add per-organization outbound-email/e-signature settings, mirroring the
-- pattern already used on insurers (see V202608201132). Stored as text to
-- accommodate AES-256-GCM ciphertext (see common/crypto).

alter table organizations add column notification_email varchar(255);
alter table organizations add column notification_email_password text;
alter table organizations add column host varchar(255);
alter table organizations add column port integer;
alter table organizations add column esignature text;
