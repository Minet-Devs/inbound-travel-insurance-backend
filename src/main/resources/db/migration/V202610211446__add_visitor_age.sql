-- Age in whole years as at date_in (start of cover). Populated on write by the
-- Visitor entity; pre-existing rows are filled by VisitorAgeBackfillRunner because
-- date_of_birth is encrypted at rest and cannot be read in SQL.
alter table visitors
    add column age integer;
