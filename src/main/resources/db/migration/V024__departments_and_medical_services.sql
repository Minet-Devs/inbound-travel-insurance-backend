-- Departments (name-only master list) and medical services. A service belongs to a
-- department by id column only (no JPA relation across features), and its name is
-- unique within its department rather than globally. Both are bulk-loaded via an
-- admin Excel import that upserts departments by name on the fly.

create table departments (
    id           uuid primary key,
    name         varchar(255) not null,
    deleted      boolean not null default false,
    created_date timestamptz,
    updated_date timestamptz,
    deleted_date timestamptz,
    created_by   uuid,
    updated_by   uuid,
    constraint uq_departments_name unique (name)
);

create table medical_services (
    id            uuid primary key,
    name          varchar(255) not null,
    department_id uuid not null,
    deleted       boolean not null default false,
    created_date  timestamptz,
    updated_date  timestamptz,
    deleted_date  timestamptz,
    created_by    uuid,
    updated_by    uuid,
    constraint uq_medical_services_name_department unique (name, department_id)
);

create index idx_medical_services_department_id on medical_services (department_id);