create table tourist_attractions (
    id           uuid primary key,
    name         varchar(255) not null,
    county       varchar(255) not null,
    deleted      boolean not null default false,
    created_date timestamptz,
    updated_date timestamptz,
    deleted_date timestamptz,
    created_by   uuid,
    updated_by   uuid,
    constraint uq_tourist_attractions_name unique (name)
);
