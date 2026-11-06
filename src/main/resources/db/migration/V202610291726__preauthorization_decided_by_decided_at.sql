-- Explicit decision audit fields, editable via PUT /api/v1/preauthorizations/{id}.
-- When null the API falls back to updated_by / updated_date for decided pre-authorizations.
alter table preauthorizations add column if not exists decided_by uuid;
alter table preauthorizations add column if not exists decided_at timestamptz;
