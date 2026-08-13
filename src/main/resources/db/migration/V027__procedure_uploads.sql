-- Excel upload tracking for the two-stage (validate -> import) procedure upload.
-- procedure_uploads is the batch record; procedure_upload_rows holds one row per
-- spreadsheet row with its validation/import outcome. upload time is the
-- BaseEntity created_date column.

create table procedure_uploads (
    id                    uuid primary key,
    original_filename     varchar(255),
    department_public_id  uuid not null,
    status                varchar(30) not null,
    total_rows            integer not null default 0,
    valid_rows            integer not null default 0,
    created_rows          integer not null default 0,
    skipped_rows          integer not null default 0,
    failed_rows           integer not null default 0,
    uploaded_by           uuid,
    processing_start_time timestamptz,
    completion_time       timestamptz,
    failure_reason        varchar(1000),
    deleted               boolean not null default false,
    created_date          timestamptz,
    updated_date          timestamptz,
    deleted_date          timestamptz,
    created_by            uuid,
    updated_by            uuid
);

create table procedure_upload_rows (
    id                          uuid primary key,
    upload_id                   uuid not null,
    excel_row_number            integer not null,
    submitted_name              varchar(500),
    submitted_description       varchar(2000),
    cleaned_name                varchar(255),
    normalized_name             varchar(255),
    row_status                  varchar(30) not null,
    error_code                  varchar(50),
    error_message               varchar(1000),
    created_procedure_public_id uuid,
    deleted                     boolean not null default false,
    created_date                timestamptz,
    updated_date                timestamptz,
    deleted_date                timestamptz,
    created_by                  uuid,
    updated_by                  uuid,
    constraint fk_procedure_upload_rows_upload
        foreign key (upload_id) references procedure_uploads (id)
);

create index idx_procedure_upload_rows_upload on procedure_upload_rows (upload_id);
