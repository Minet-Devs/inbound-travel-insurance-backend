-- Tracks when the activation email went out, so it is sent at most once when benefits are
-- assigned after activation. Existing ACTIVE visitors already received it (or are covered by
-- the benefit back-fill), so mark them sent to avoid re-emailing on future benefit changes.
alter table visitors add column activation_email_sent_at timestamptz;

update visitors set activation_email_sent_at = now() where visitor_status = 'ACTIVE';
