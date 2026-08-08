-- Extends visitor KYC onboarding data to match the Ministry of Health
-- e-portal ("Kenya Cares") requirements (§8.1): address, a face photo
-- upload (stored as a URL, not the binary), reason for travel, and free-text
-- underlying conditions/prescribed medicines (optional). Existing rows get a
-- placeholder for the three mandatory new fields.

alter table visitors
    add column address varchar(255) not null default '',
    add column reason_for_travel varchar(255) not null default '',
    add column face_photo_url varchar(2000) not null default '',
    add column underlying_conditions varchar(1000);
