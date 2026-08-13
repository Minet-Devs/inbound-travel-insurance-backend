-- Procedure catalogue: service/catalogue items (e.g. "Nebulization") referenced
-- to a department by public id only (no FK relationship to the department feature).
-- Codes come from a dedicated sequence; uniqueness is enforced on the code and on
-- department_public_id + normalized_name (partial index, excludes soft-deleted rows).

create sequence procedure_code_seq start with 1 increment by 1;

create table procedures (
    id                     uuid primary key,
    procedure_code         varchar(20) not null,
    name                   varchar(255) not null,
    normalized_name        varchar(255) not null,
    description            varchar(2000),
    department_public_id   uuid not null,
    active                 boolean not null default true,
    upload_batch_public_id uuid,
    deleted                boolean not null default false,
    created_date           timestamptz,
    updated_date           timestamptz,
    deleted_date           timestamptz,
    created_by             uuid,
    updated_by             uuid,
    constraint uq_procedures_code unique (procedure_code)
);

-- Duplicate rule: one active-or-inactive procedure per department + normalized name,
-- ignoring soft-deleted rows.
create unique index uq_procedures_department_normalized_name
    on procedures (department_public_id, normalized_name)
    where deleted = false;

create index idx_procedures_department on procedures (department_public_id);
create index idx_procedures_name on procedures (name);
create index idx_procedures_upload_batch on procedures (upload_batch_public_id);
