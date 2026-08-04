-- Biometric verification requests (eKYC/ecitizen integration). Persists the
-- state that the old Node middleware kept in memory: the subject being verified,
-- the eKYC request id + embedded iframe details returned by the trigger, and the
-- async result delivered via the notification callback.

create table biometric_verifications (
    id                  uuid primary key,
    subject_id_number   varchar(100) not null,
    subject_id_type     varchar(50) not null,
    policy_number       varchar(100) not null,
    workstation_id      varchar(255) not null,
    ekyc_request_id     varchar(100),
    embeded_token       varchar(4000),
    embeded_expiry      varchar(100),
    request_url         varchar(2000),
    status              varchar(30) not null,
    result              varchar(50),
    status_code         varchar(100),
    remaining_attempts  integer,
    deleted             boolean not null default false,
    created_date        timestamptz,
    updated_date        timestamptz,
    deleted_date        timestamptz,
    created_by          uuid,
    updated_by          uuid
);

create index idx_biometric_verifications_ekyc_request_id on biometric_verifications (ekyc_request_id);
create index idx_biometric_verifications_policy_number on biometric_verifications (policy_number);
