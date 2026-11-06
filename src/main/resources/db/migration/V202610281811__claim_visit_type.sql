-- Optional visit type (OUTPATIENT / INPATIENT) on a claim.
alter table claims add column if not exists visit_type varchar(20);
