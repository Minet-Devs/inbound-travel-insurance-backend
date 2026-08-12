-- ICD-11 diagnosis codes reference table (code + title), bulk-loaded via the
-- admin Excel import endpoint. Carries the BaseEntity audit/soft-delete columns.
-- The code is unique so re-imports upsert by code.

create table icd11_codes (
    id           uuid primary key,
    code         varchar(50) not null,
    title        varchar(1000) not null,
    deleted      boolean not null default false,
    created_date timestamptz,
    updated_date timestamptz,
    deleted_date timestamptz,
    created_by   uuid,
    updated_by   uuid,
    constraint uq_icd11_codes_code unique (code)
);

create index idx_icd11_codes_title on icd11_codes (title);
