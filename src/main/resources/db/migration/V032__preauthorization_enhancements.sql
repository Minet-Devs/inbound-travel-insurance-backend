alter table preauthorizations drop column medical_service_id;

create table preauthorization_enhancements (
    id                   uuid primary key,
    preauthorization_id  uuid not null references preauthorizations (id),
    medical_service_id   uuid,
    requested_amount     numeric(15, 2) not null,
    deleted              boolean not null default false,
    created_date         timestamptz,
    updated_date         timestamptz,
    deleted_date         timestamptz,
    created_by           uuid,
    updated_by           uuid,
    constraint uq_preauthorization_enhancements_preauthorization_id unique (preauthorization_id)
);

alter table preauthorization_items rename column preauthorization_id to enhancement_id;
alter table preauthorization_items drop constraint preauthorization_items_preauthorization_id_fkey;
alter table preauthorization_items
    add constraint preauthorization_items_enhancement_id_fkey
    foreign key (enhancement_id) references preauthorization_enhancements (id);

drop index idx_preauthorization_items_preauthorization_id;
create index idx_preauthorization_items_enhancement_id on preauthorization_items (enhancement_id);