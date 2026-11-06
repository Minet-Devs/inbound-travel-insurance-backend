-- Optional visit type (OUTPATIENT / INPATIENT) on a pre-authorization.
alter table preauthorizations add column if not exists visit_type varchar(20);
