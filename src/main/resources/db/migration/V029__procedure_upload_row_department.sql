-- The procedure Excel upload now carries a Department column per row: each row's
-- department name is resolved (case-insensitively) to a department id, replacing
-- the single upload-level department.

alter table procedure_upload_rows add column submitted_department varchar(255);
alter table procedure_upload_rows add column department_public_id uuid;

alter table procedure_uploads drop column department_public_id;
