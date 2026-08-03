-- Visitor: the insured traveler behind a policy (passport-based KYC), and
-- visitor_benefits: catalog benefits assigned to a visitor with a snapshotted
-- limit. Cross-feature references are plain UUID columns.

create table visitors (
    id               uuid primary key,
    policy_id        uuid not null,
    full_name        varchar(255) not null,
    passport_number  varchar(100) not null,
    date_of_birth    date not null,
    gender           varchar(20) not null,
    nationality      varchar(100) not null,
    email            varchar(255) not null,
    phone_number     varchar(50) not null,
    date_in          date not null,
    date_out         date not null,
    marital_status   varchar(20) not null,
    next_of_kin_name varchar(255),
    next_of_kin_phone varchar(50),
    deleted          boolean not null default false,
    created_date     timestamptz,
    updated_date     timestamptz,
    deleted_date     timestamptz,
    created_by       uuid,
    updated_by       uuid
);

-- One live visitor per policy, and passport numbers unique among live rows;
-- soft-deleted rows may be re-created.
create unique index uq_visitors_policy_id
    on visitors (policy_id)
    where deleted = false;

create unique index uq_visitors_passport_number
    on visitors (lower(passport_number))
    where deleted = false;

create table visitor_benefits (
    id           uuid primary key,
    visitor_id   uuid not null,
    benefit_id   uuid not null,
    limit_amount numeric(15, 2) not null,
    deleted      boolean not null default false,
    created_date timestamptz,
    updated_date timestamptz,
    deleted_date timestamptz,
    created_by   uuid,
    updated_by   uuid
);

create index idx_visitor_benefits_visitor_id on visitor_benefits (visitor_id);

-- A visitor may hold each catalog benefit at most once among live rows.
create unique index uq_visitor_benefits_visitor_id_benefit_id
    on visitor_benefits (visitor_id, benefit_id)
    where deleted = false;
