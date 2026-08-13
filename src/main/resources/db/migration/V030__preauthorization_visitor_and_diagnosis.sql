-- Idempotent: this migration's DDL was originally shipped as V024 and applied to
-- some databases before the version was renumbered to V030. Guard every statement
-- so it succeeds whether or not the columns/indexes already exist.
alter table preauthorizations
    add column if not exists visitor_id uuid,
    add column if not exists icd11_code_id uuid;

create index if not exists idx_preauthorizations_visitor_id on preauthorizations (visitor_id);
create index if not exists idx_preauthorizations_icd11_code_id on preauthorizations (icd11_code_id);
