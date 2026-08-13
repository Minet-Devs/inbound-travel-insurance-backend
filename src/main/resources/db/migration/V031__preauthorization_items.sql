alter table preauthorizations add column medical_service_id uuid;

create table preauthorization_items (
    id                   uuid primary key,
    preauthorization_id  uuid not null references preauthorizations (id),
    description          varchar(1000) not null,
    quantity              numeric(15, 2) not null,
    unit_price            numeric(15, 2) not null,
    amount                numeric(15, 2) not null,
    service_date          date,
    deleted               boolean not null default false,
    created_date          timestamptz,
    updated_date          timestamptz,
    deleted_date          timestamptz,
    created_by            uuid,
    updated_by            uuid
);

create index idx_preauthorization_items_preauthorization_id on preauthorization_items (preauthorization_id);