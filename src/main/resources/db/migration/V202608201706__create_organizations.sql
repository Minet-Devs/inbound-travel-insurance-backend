create table organizations (
    id           uuid primary key,
    name         varchar(255) not null,
    email        varchar(255) not null,
    phone_number varchar(255),
    address      varchar(255),
    city         varchar(255),
    deleted      boolean not null default false,
    created_date timestamptz,
    updated_date timestamptz,
    deleted_date timestamptz,
    created_by   uuid,
    updated_by   uuid,
    constraint uq_organizations_name unique (name)
);
