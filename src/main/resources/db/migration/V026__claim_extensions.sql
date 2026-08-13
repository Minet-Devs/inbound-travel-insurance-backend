-- Augment claims with visitor/insurer references, free-form diagnosis and
-- procedure UUID arrays, invoice and document references, and a free-text
-- prescription. Adds the invoices and invoice_items tables.
--
-- The claim's diagnosis/procedure/invoice/document ID sets are plain UUID
-- element collections (join tables), following the existing pattern used for
-- policy_insurers and user_roles — not JPA relations. Invoices are standalone
-- aggregates owned by a claim via claim_id. Document IDs are a placeholder
-- for a future upload service; the claim_documents join table persists them.

alter table claims add column visitor_id uuid;
alter table claims add column insurer_id uuid;
alter table claims add column prescription varchar(2000);

create index idx_claims_visitor_id on claims (visitor_id);
create index idx_claims_insurer_id on claims (insurer_id);

create table claim_diagnoses (
    claim_id     uuid not null references claims (id),
    diagnosis_id uuid not null,
    primary key (claim_id, diagnosis_id)
);

create table claim_procedures (
    claim_id     uuid not null references claims (id),
    procedure_id uuid not null,
    primary key (claim_id, procedure_id)
);

create table claim_invoices (
    claim_id   uuid not null references claims (id),
    invoice_id uuid not null,
    primary key (claim_id, invoice_id)
);

create table claim_documents (
    claim_id    uuid not null references claims (id),
    document_id uuid not null,
    primary key (claim_id, document_id)
);

create table invoices (
    id             uuid primary key,
    claim_id       uuid not null references claims (id),
    invoice_number varchar(100),
    issue_date     date,
    currency       varchar(10),
    total_amount   numeric(15, 2) not null,
    deleted        boolean not null default false,
    created_date   timestamptz,
    updated_date   timestamptz,
    deleted_date   timestamptz,
    created_by     uuid,
    updated_by     uuid
);

create table invoice_items (
    id           uuid primary key,
    invoice_id   uuid not null references invoices (id),
    description  varchar(1000) not null,
    quantity     numeric(15, 2) not null,
    unit_price   numeric(15, 2) not null,
    amount       numeric(15, 2) not null,
    service_date date,
    deleted      boolean not null default false,
    created_date timestamptz,
    updated_date timestamptz,
    deleted_date timestamptz,
    created_by   uuid,
    updated_by   uuid
);

create index idx_invoices_claim_id on invoices (claim_id);
create index idx_invoice_items_invoice_id on invoice_items (invoice_id);
