create table premium_receipts (
    id                     uuid primary key,
    total_premium          numeric(15, 2) not null,
    pcf_levy               numeric(10, 6) not null,
    insurance_premium_levy numeric(10, 6) not null,
    stamp_duty             numeric(15, 2) not null,
    training_levy          numeric(10, 6) not null,
    deleted                boolean not null default false,
    created_date           timestamptz,
    updated_date           timestamptz,
    deleted_date           timestamptz,
    created_by             uuid,
    updated_by             uuid
);

create unique index one_row_only on premium_receipts ((true));
