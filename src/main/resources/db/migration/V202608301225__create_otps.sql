create table otps (
    id                 uuid primary key,
    otp                varchar(6) not null,
    expiry_time        timestamptz not null,
    email              varchar(255) not null,
    service_provider_id uuid not null,
    deleted            boolean not null default false,
    created_date       timestamptz,
    updated_date       timestamptz,
    deleted_date       timestamptz,
    created_by         uuid,
    updated_by         uuid
);

create index idx_otps_email_service_provider_id on otps (email, service_provider_id);
