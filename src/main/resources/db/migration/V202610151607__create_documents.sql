-- Uploaded claim / pre-authorization documents. One row per upload: the uploaded PDFs are merged
-- into a single PDF stored in blob storage; source_files keeps metadata about the merged inputs.
-- Supersedes the claim_documents placeholder join table (kept for now, see backend-architecture.md).
create table documents (
    id                           uuid primary key,
    owner_type                   varchar(30)  not null,
    owner_id                     uuid         not null,
    document_type                varchar(30)  not null,
    filename                     varchar(255) not null,
    content_type                 varchar(100) not null,
    page_count                   integer      not null,
    size_bytes                   bigint       not null,
    sha256                       varchar(64)  not null,
    storage_key                  varchar(255) not null,
    source_files                 jsonb        not null default '[]'::jsonb,
    uploaded_by                  uuid,
    uploaded_by_organization_id  uuid,
    deleted                      boolean      not null default false,
    created_date                 timestamptz,
    updated_date                 timestamptz,
    deleted_date                 timestamptz,
    created_by                   uuid,
    updated_by                   uuid
);

create index idx_documents_owner on documents (owner_type, owner_id);
