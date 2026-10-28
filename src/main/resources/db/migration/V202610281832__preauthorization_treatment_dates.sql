-- Treatment / discharge dates on a pre-authorization (populated by the Excel offline-approval upload).
-- icd11_code_id is already nullable at the database level; the entity now allows it to be empty.
alter table preauthorizations add column if not exists treatment_date date;
alter table preauthorizations add column if not exists discharge_date date;
