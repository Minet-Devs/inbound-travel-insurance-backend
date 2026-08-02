-- Initial schema: users, insurers, service providers, policies, benefits,
-- preauthorizations, claims. Every table carries the BaseEntity audit and
-- soft-delete columns. Cross-feature references are plain UUID columns.

create table users (
    id            uuid primary key,
    first_name    varchar(255) not null,
    last_name     varchar(255) not null,
    email         varchar(255) not null,
    password      varchar(255) not null,
    phone_number  varchar(50),
    organization_id uuid,
    deleted       boolean not null default false,
    created_date  timestamptz,
    updated_date  timestamptz,
    deleted_date  timestamptz,
    created_by    uuid,
    updated_by    uuid,
    constraint uq_users_email unique (email)
);

create table user_roles (
    user_id uuid not null references users (id),
    role    varchar(50) not null,
    primary key (user_id, role)
);

create table insurers (
    id            uuid primary key,
    name          varchar(255) not null,
    contact_email varchar(255) not null,
    contact_phone varchar(50),
    address       varchar(500),
    deleted       boolean not null default false,
    created_date  timestamptz,
    updated_date  timestamptz,
    deleted_date  timestamptz,
    created_by    uuid,
    updated_by    uuid,
    constraint uq_insurers_name unique (name)
);

create table service_providers (
    id            uuid primary key,
    name          varchar(255) not null,
    contact_email varchar(255) not null,
    contact_phone varchar(50),
    address       varchar(500),
    country       varchar(100),
    deleted       boolean not null default false,
    created_date  timestamptz,
    updated_date  timestamptz,
    deleted_date  timestamptz,
    created_by    uuid,
    updated_by    uuid,
    constraint uq_service_providers_name unique (name)
);

create table policies (
    id               uuid primary key,
    policy_number    varchar(100) not null,
    insurer_id       uuid not null,
    customer_id      uuid not null,
    cover_start_date date not null,
    cover_end_date   date not null,
    status           varchar(20) not null,
    deleted          boolean not null default false,
    created_date     timestamptz,
    updated_date     timestamptz,
    deleted_date     timestamptz,
    created_by       uuid,
    updated_by       uuid,
    constraint uq_policies_policy_number unique (policy_number)
);

create index idx_policies_insurer_id on policies (insurer_id);
create index idx_policies_customer_id on policies (customer_id);

create table benefits (
    id           uuid primary key,
    policy_id    uuid not null,
    name         varchar(255) not null,
    limit_amount numeric(15, 2) not null,
    used_amount  numeric(15, 2) not null default 0,
    deleted      boolean not null default false,
    created_date timestamptz,
    updated_date timestamptz,
    deleted_date timestamptz,
    created_by   uuid,
    updated_by   uuid
);

create index idx_benefits_policy_id on benefits (policy_id);

create table preauthorizations (
    id                  uuid primary key,
    policy_id           uuid not null,
    benefit_id          uuid not null,
    service_provider_id uuid not null,
    requested_amount    numeric(15, 2) not null,
    approved_amount     numeric(15, 2),
    service_description varchar(1000),
    decision_reason     varchar(1000),
    status              varchar(30) not null,
    deleted             boolean not null default false,
    created_date        timestamptz,
    updated_date        timestamptz,
    deleted_date        timestamptz,
    created_by          uuid,
    updated_by          uuid
);

create index idx_preauthorizations_policy_id on preauthorizations (policy_id);
create index idx_preauthorizations_service_provider_id on preauthorizations (service_provider_id);

create table claims (
    id                  uuid primary key,
    policy_id           uuid not null,
    benefit_id          uuid not null,
    service_provider_id uuid,
    preauthorization_id uuid,
    claimed_amount      numeric(15, 2) not null,
    approved_amount     numeric(15, 2),
    description         varchar(1000),
    decision_reason     varchar(1000),
    status              varchar(30) not null,
    deleted             boolean not null default false,
    created_date        timestamptz,
    updated_date        timestamptz,
    deleted_date        timestamptz,
    created_by          uuid,
    updated_by          uuid
);

create index idx_claims_policy_id on claims (policy_id);
create index idx_claims_service_provider_id on claims (service_provider_id);
