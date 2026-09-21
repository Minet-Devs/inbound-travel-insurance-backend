create table visitor_otps (
    id           uuid primary key,
    otp          varchar(6) not null,
    expiry_time  timestamptz not null,
    email        varchar(255) not null,
    deleted      boolean not null default false,
    created_date timestamptz,
    updated_date timestamptz,
    deleted_date timestamptz,
    created_by   uuid,
    updated_by   uuid
);

create index idx_visitor_otps_email on visitor_otps (email);
